/*
 * Copyright (C) 2021-2022 Rick Busarow
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
import modulecheck.dagger.TaskScope
import modulecheck.gradle.platforms.KotlinEnvironmentFactory
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.kotlin.compiler.KotlinEnvironment
import modulecheck.parsing.kotlin.compiler.internal.isKotlinFile
import modulecheck.reporting.logging.McLogger
import modulecheck.utils.coroutines.flatMapSetMerge
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.ResetManager
import modulecheck.utils.lazy.lazyDeferred
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns.Kind.FROM_DEPENDENCIES
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
import org.jetbrains.kotlin.context.ContextForNewModule
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import java.io.File
import javax.inject.Inject

/**
 * @property projectPath path of the associated Gradle project
 * @property sourceSetName name of the associated
 *     [SourceSet][modulecheck.model.dependency.McSourceSet]
 * @property classpathFiles `.jar` files from external dependencies
 * @property sourceDirs all jvm source code directories for this source set, like
 *     `[...]/myProject/src/main/java`.
 * @property kotlinLanguageVersion the version of Kotlin being used
 * @property jvmTarget the version of Java being compiled to
 * @property safeAnalysisResultAccess provides thread-safe, "leased" access to the ModuleDescriptors
 *     of dependencies, since only one downstream project can safely consume (and update the cache
 *     of) a descriptor at any given time
 * @property logger logs Kotlin compiler messages during analysis
 * @property resetManager used to reset caching
 * @since 0.13.0
 */
@Suppress("LongParameterList")
class RealKotlinEnvironment(
  val projectPath: StringProjectPath,
  val sourceSetName: SourceSetName,
  val classpathFiles: LazyDeferred<List<File>>,
  private val sourceDirs: Collection<File>,
  val kotlinLanguageVersion: LanguageVersion?,
  val jvmTarget: JvmTarget,
  val safeAnalysisResultAccess: SafeAnalysisResultAccess,
  val logger: McLogger,
  private val resetManager: ResetManager
) : KotlinEnvironment {

  private val sourceFiles by lazy {
    sourceDirs.asSequence()
      .flatMap { dir -> dir.walkTopDown() }
      .filter { it.isFile }
      .toSet()
  }

  override val compilerConfiguration = lazyDeferred {
    createCompilerConfiguration(
      classpathFiles = classpathFiles.value.toList(),
      sourceFiles = sourceFiles.toList(),
      kotlinLanguageVersion = kotlinLanguageVersion,
      jvmTarget = jvmTarget
    )
  }

  override val coreEnvironment = lazyDeferred {
    createKotlinCoreEnvironment(compilerConfiguration.await())
  }

  override val lightPsiFactory = lazyDeferred {
    RealMcPsiFileFactory(this)
  }

  override val heavyPsiFactory = lazyDeferred {
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

    safeAnalysisResultAccess.withLeases(
      requester = this,
      projectPath = projectPath,
      sourceSetName = sourceSetName
    ) { dependencyEnvironments ->

      @Suppress("UNCHECKED_CAST")
      val descriptors = dependencyEnvironments
        .flatMapSetMerge { dependency ->
          val directDependency = dependency.moduleDescriptorDeferred.await()
          setOf(directDependency)
            .plus(directDependency.allDependencyModules as List<ModuleDescriptorImpl>)
        }
        .toList()

      createAnalysisResult(
        coreEnvironment = coreEnvironment.await(),
        ktFiles = ktFiles,
        dependencyModuleDescriptors = descriptors
      )
    }
  }

  override val bindingContextDeferred = lazyDeferred {
    analysisResultDeferred.await().bindingContext
  }

  override val moduleDescriptorDeferred: LazyDeferred<ModuleDescriptorImpl> = lazyDeferred {
    val golden = analysisResultDeferred.await().moduleDescriptor as ModuleDescriptorImpl
    golden.copy()
  }

  private suspend fun ModuleDescriptorImpl.copy(): ModuleDescriptorImpl {
    val project = coreEnvironment.await().project

    val projectContext = ProjectContext(project, "testing project context")
    val builtIns = JvmBuiltIns(projectContext.storageManager, FROM_DEPENDENCIES)

    val mutableModuleContext = ContextForNewModule(
      projectContext,
      Name.special(
        "<${projectPath.value}:${sourceSetName.value} ${kotlin.random.Random.nextInt()}>"
      ),
      builtIns,
      JvmPlatforms.jvmPlatformByTargetVersion(jvmTarget)
    )

    mutableModuleContext.module.setDependencies(
      allDependencyModules.filterIsInstance<ModuleDescriptorImpl>()
    )

    mutableModuleContext.module.initialize(this.packageFragmentProvider)

    return mutableModuleContext.module
  }

  private val messageCollector by lazy {
    McMessageCollector(
      messageRenderer = MessageRenderer.GRADLE_STYLE,
      logger = logger,
      logLevel = McMessageCollector.LogLevel.WARNINGS_AS_ERRORS
    )
  }

  private fun createAnalysisResult(
    coreEnvironment: KotlinCoreEnvironment,
    ktFiles: List<KtFile>,
    dependencyModuleDescriptors: List<ModuleDescriptorImpl>
  ): AnalysisResult {

    val analyzer = AnalyzerWithCompilerReport(
      messageCollector,
      coreEnvironment.configuration.languageVersionSettings
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

    return analyzer.analysisResult
  }

  private suspend fun createCompilerConfiguration(
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
      addJvmClasspathRoots(classpathFiles.await())
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

  /**
   * Dagger implementation for [KotlinEnvironmentFactory]
   *
   * @since 0.13.0
   */
  @ContributesBinding(TaskScope::class)
  class Factory @Inject constructor(
    private val safeAnalysisResultAccess: SafeAnalysisResultAccess,
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
      safeAnalysisResultAccess = safeAnalysisResultAccess,
      logger = logger,
      resetManager = ResetManager()
    )
  }
}
