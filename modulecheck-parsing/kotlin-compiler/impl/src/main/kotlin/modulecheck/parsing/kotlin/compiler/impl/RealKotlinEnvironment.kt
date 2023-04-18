/*
 * Copyright (C) 2021-2023 Rick Busarow
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package modulecheck.parsing.kotlin.compiler.impl

import com.squareup.anvil.annotations.ContributesBinding
import dispatch.core.withIO
import modulecheck.dagger.TaskScope
import modulecheck.gradle.platforms.KotlinEnvironmentFactory
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.kotlin.compiler.KotlinEnvironment
import modulecheck.parsing.kotlin.compiler.internal.isKotlinFile
import modulecheck.reporting.logging.McLogger
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.ResetManager
import modulecheck.utils.lazy.lazyDeferred
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles.JVM_CONFIG_FILES
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoots
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import java.io.File
import javax.inject.Inject

/**
 * @property projectPath path of the associated Gradle project
 * @property sourceSetName name of the associated
 *   [SourceSet][modulecheck.model.dependency.McSourceSet]
 * @property classpathFiles `.jar` files from external dependencies
 * @property sourceDirs all jvm source code directories for
 *   this source set, like `[...]/myProject/src/main/java`.
 * @property kotlinLanguageVersion the version of Kotlin being used
 * @property jvmTarget the version of Java being compiled to
 * @property dependencyModuleDescriptorAccess provides the module descriptors of
 *   all dependency source sets from the current module and dependency modules
 * @property logger logs Kotlin compiler messages during analysis
 * @property resetManager used to reset caching
 */
@Suppress("LongParameterList")
class RealKotlinEnvironment(
  val projectPath: StringProjectPath,
  val sourceSetName: SourceSetName,
  val classpathFiles: LazyDeferred<List<File>>,
  private val sourceDirs: Collection<File>,
  val kotlinLanguageVersion: LanguageVersion?,
  val jvmTarget: JvmTarget,
  val dependencyModuleDescriptorAccess: DependencyModuleDescriptorAccess,
  val logger: McLogger,
  private val resetManager: ResetManager
) : KotlinEnvironment {

  private val sourceFiles by lazy {
    sourceDirs.asSequence()
      .flatMap { dir -> dir.walkTopDown() }
      .filter { it.isFile }
      .toSet()
  }

  override val compilerConfiguration: LazyDeferred<CompilerConfiguration> = lazyDeferred {
    createCompilerConfiguration(
      // TODO re-enable classpath files once external dependency resolution is working
      // classpathFiles =   classpathFiles.await(),
      classpathFiles = emptyList(),
      sourceFiles = sourceFiles.toList(),
      kotlinLanguageVersion = kotlinLanguageVersion,
      jvmTarget = jvmTarget
    )
  }

  override val coreEnvironment: LazyDeferred<KotlinCoreEnvironment> = lazyDeferred {
    createKotlinCoreEnvironment(compilerConfiguration.await())
  }

  override val lightPsiFactory: LazyDeferred<RealMcPsiFileFactory> = lazyDeferred {
    RealMcPsiFileFactory(this)
  }

  override val heavyPsiFactory: LazyDeferred<RealMcPsiFileFactory> = lazyDeferred {
    analysisResultDeferred.await()
    RealMcPsiFileFactory(this)
  }

  override suspend fun bestAvailablePsiFactory(): RealMcPsiFileFactory {
    return when {
      heavyPsiFactory.isCompleted -> heavyPsiFactory.getCompleted()
      analysisResultDeferred.isCompleted -> heavyPsiFactory.await()
      else -> lightPsiFactory.await()
    }
  }

  private val kotlinSourceFiles by lazy { sourceFiles.filter { it.isKotlinFile() } }

  override suspend fun javaPsiFile(file: File): PsiJavaFile {
    // Type resolution for Java Psi files assumes that analysis has already been run.
    // Otherwise, we get:
    // `UninitializedPropertyAccessException: lateinit property module has not been initialized`
    analysisResultDeferred.await()
    return heavyPsiFactory.await().createJava(file)
  }

  override suspend fun ktFile(file: File): KtFile {
    return bestAvailablePsiFactory().createKotlin(file)
  }

  override val analysisResultDeferred: LazyDeferred<AnalysisResult> = lazyDeferred {

    val psiFactory = lightPsiFactory.await()

    val ktFiles = kotlinSourceFiles
      .map { file -> psiFactory.createKotlin(file) }

    val descriptors = dependencyModuleDescriptorAccess.dependencyModuleDescriptors(
      projectPath = projectPath,
      sourceSetName = sourceSetName
    )

    createAnalysisResult(
      coreEnvironment = coreEnvironment.await(),
      ktFiles = ktFiles,
      dependencyModuleDescriptors = descriptors
    )
  }

  override val bindingContextDeferred: LazyDeferred<BindingContext> = lazyDeferred {
    analysisResultDeferred.await().bindingContext
  }

  override val moduleDescriptorDeferred: LazyDeferred<ModuleDescriptorImpl> = lazyDeferred {
    analysisResultDeferred.await().moduleDescriptor as ModuleDescriptorImpl
  }

  private val messageCollector by lazy {
    McMessageCollector(
      messageRenderer = MessageRenderer.GRADLE_STYLE,
      logger = logger,
      logLevel = McMessageCollector.LogLevel.WARNINGS_AS_ERRORS
    )
  }

  private suspend fun createAnalysisResult(
    coreEnvironment: KotlinCoreEnvironment,
    ktFiles: List<KtFile>,
    dependencyModuleDescriptors: List<ModuleDescriptorImpl>
  ): AnalysisResult = withIO {

    val analyzer = AnalyzerWithCompilerReport(
      messageCollector = messageCollector,
      languageVersionSettings = coreEnvironment.configuration.languageVersionSettings,
      renderDiagnosticName = false
    )

    analyzer.analyzeAndReport(ktFiles) {
      TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
        project = coreEnvironment.project,
        files = ktFiles,
        trace = NoScopeRecordCliBindingTrace(),
        configuration = coreEnvironment.configuration,
        packagePartProvider = coreEnvironment::createPackagePartProvider,
        declarationProviderFactory = ::FileBasedDeclarationProviderFactory,
        explicitModuleDependencyList = dependencyModuleDescriptors
      )
    }

    messageCollector.printIssuesCountIfAny()

    analyzer.analysisResult
  }

  private fun createCompilerConfiguration(
    classpathFiles: List<File>,
    sourceFiles: List<File>,
    kotlinLanguageVersion: LanguageVersion?,
    jvmTarget: JvmTarget
  ): CompilerConfiguration {
    val javaFiles = mutableListOf<File>()
    val kotlinFiles = mutableListOf<String>()

    sourceFiles.forEach { file ->
      when {
        file.isKotlinFile() -> kotlinFiles.add(file.absolutePath)
        file.isJavaFile() -> javaFiles.add(file)
      }
    }

    return CompilerConfiguration().apply {
      put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
      put(JVMConfigurationKeys.JVM_TARGET, jvmTarget)
      put(CommonConfigurationKeys.MODULE_NAME, "${projectPath.value}:${sourceSetName.value}")

      if (kotlinLanguageVersion != null) {
        val languageVersionSettings = LanguageVersionSettingsImpl(
          languageVersion = kotlinLanguageVersion,
          apiVersion = ApiVersion.createByLanguageVersion(kotlinLanguageVersion)
        )
        put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, languageVersionSettings)
      }

      put(JVMConfigurationKeys.JVM_TARGET, jvmTarget)

      addJavaSourceRoots(javaFiles)
      addKotlinSourceRoots(kotlinFiles)
      addJvmClasspathRoots(classpathFiles)
    }
  }

  private fun createKotlinCoreEnvironment(
    configuration: CompilerConfiguration
  ): KotlinCoreEnvironment {
    // https://github.com/JetBrains/kotlin/commit/2568804eaa2c8f6b10b735777218c81af62919c1
    setIdeaIoUseFallback()

    return KotlinCoreEnvironment.createForProduction(
      parentDisposable = resetManager,
      configuration = configuration,
      configFiles = JVM_CONFIG_FILES
    )
  }

  /** Dagger implementation for [KotlinEnvironmentFactory] */
  @ContributesBinding(TaskScope::class)
  class Factory @Inject constructor(
    private val dependencyModuleDescriptorAccess: DependencyModuleDescriptorAccess,
    private val logger: McLogger
  ) : KotlinEnvironmentFactory {
    override fun create(
      projectPath: StringProjectPath,
      sourceSetName: SourceSetName,
      classpathFiles: LazyDeferred<List<File>>,
      sourceDirs: Collection<File>,
      kotlinLanguageVersion: LanguageVersion?,
      jvmTarget: JvmTarget
    ): RealKotlinEnvironment = RealKotlinEnvironment(
      projectPath = projectPath,
      sourceSetName = sourceSetName,
      classpathFiles = classpathFiles,
      sourceDirs = sourceDirs,
      kotlinLanguageVersion = kotlinLanguageVersion,
      jvmTarget = jvmTarget,
      dependencyModuleDescriptorAccess = dependencyModuleDescriptorAccess,
      logger = logger,
      resetManager = ResetManager()
    )
  }
}
