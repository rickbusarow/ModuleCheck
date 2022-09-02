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
import kotlinx.coroutines.flow.toList
import modulecheck.dagger.TaskScope
import modulecheck.gradle.platforms.KotlinEnvironmentFactory
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.kotlin.compiler.KotlinEnvironment
import modulecheck.parsing.kotlin.compiler.internal.isKotlinFile
import modulecheck.utils.coroutines.mapAsync
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.lazyDeferred
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoots
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.kotlin.com.intellij.pom.PomModel
import org.jetbrains.kotlin.com.intellij.pom.PomModelAspect
import org.jetbrains.kotlin.com.intellij.pom.PomTransaction
import org.jetbrains.kotlin.com.intellij.pom.impl.PomTransactionBase
import org.jetbrains.kotlin.com.intellij.pom.tree.TreeAspect
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import sun.reflect.ReflectionFactory
import java.io.File
import javax.inject.Inject

/**
 * @property projectPath path of the associated Gradle project
 * @property sourceSetName name of the associated
 *   [SourceSet][modulecheck.parsing.gradle.model.McSourceSet]
 * @property classpathFiles `.jar` files from external dependencies
 * @property sourceDirs all jvm source code directories for this source set, like
 *   `[...]/myProject/src/main/java`.
 * @property kotlinLanguageVersion the version of Kotlin being used
 * @property jvmTarget the version of Java being compiled to
 * @property safeAnalysisResultAccess provides thread-safe, "leased" access to the ModuleDescriptors
 *   of dependencies, since only one downstream project can safely
 *   consume (and update the cache of) a descriptor at any given time
 * @since 0.13.0
 */
@Suppress("LongParameterList")
class RealKotlinEnvironment(
  val projectPath: StringProjectPath,
  val sourceSetName: SourceSetName,
  val classpathFiles: Lazy<Collection<File>>,
  private val sourceDirs: Collection<File>,
  val kotlinLanguageVersion: LanguageVersion?,
  private val jvmTarget: JvmTarget,
  val safeAnalysisResultAccess: SafeAnalysisResultAccess
) : KotlinEnvironment {

  private val sourceFiles by lazy {
    sourceDirs.asSequence()
      .flatMap { dir -> dir.walkTopDown() }
      .filter { it.isFile }
      .toSet()
  }

  override val compilerConfiguration by lazy {
    createCompilerConfiguration(
      classpathFiles = classpathFiles.value.toList(),
      sourceFiles = sourceFiles.toList(),
      kotlinLanguageVersion = kotlinLanguageVersion,
      jvmTarget = jvmTarget
    )
  }

  override val coreEnvironment by lazy { createKotlinCoreEnvironment(compilerConfiguration) }

  override val psiFileFactory by lazy { RealMcPsiFileFactory(this) }

  override val javaFiles by lazy {
    sourceFiles.filter { it.isJavaFile() }
      .associateWith { file -> psiFileFactory.createJava(file) }
  }
  override val ktFiles by lazy {
    sourceFiles.filter { it.isKotlinFile() }
      .associateWith { file -> psiFileFactory.createKotlin(file) }
  }

  override val analysisResultDeferred: LazyDeferred<AnalysisResult> = lazyDeferred {

    safeAnalysisResultAccess.withLeases(
      requester = this,
      projectPath = projectPath,
      sourceSetName = sourceSetName
    ) { dependencyEnvironments ->

      val descriptors = dependencyEnvironments
        .mapAsync { dependency ->
          dependency.moduleDescriptorDeferred.await()
        }
        .toList()

      maybeCreateAnalysisResult(
        coreEnvironment = coreEnvironment,
        ktFiles = ktFiles.values.toList(),
        dependencyModuleDescriptors = descriptors
      )
    }
  }

  override val bindingContextDeferred = lazyDeferred {
    analysisResultDeferred.await().bindingContext
  }

  override val moduleDescriptorDeferred: LazyDeferred<ModuleDescriptorImpl> = lazyDeferred {
    analysisResultDeferred.await().moduleDescriptor as ModuleDescriptorImpl
  }

  /**
   * Dagger implementation for [KotlinEnvironmentFactory]
   *
   * @since 0.13.0
   */
  @ContributesBinding(TaskScope::class)
  class Factory @Inject constructor(
    private val safeAnalysisResultAccess: SafeAnalysisResultAccess
  ) : KotlinEnvironmentFactory {
    override fun create(
      projectPath: StringProjectPath,
      sourceSetName: SourceSetName,
      classpathFiles: Lazy<Collection<File>>,
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
      safeAnalysisResultAccess = safeAnalysisResultAccess
    )
  }
}

private fun maybeCreateAnalysisResult(
  coreEnvironment: KotlinCoreEnvironment,
  ktFiles: List<KtFile>,
  dependencyModuleDescriptors: List<ModuleDescriptorImpl>
): AnalysisResult {
  return VersionNeutralTopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
    project = coreEnvironment.project,
    files = ktFiles,
    trace = NoScopeRecordCliBindingTrace(),
    configuration = coreEnvironment.configuration,
    packagePartProvider = coreEnvironment::createPackagePartProvider,
    declarationProviderFactory = ::FileBasedDeclarationProviderFactory,
    explicitModuleDependencyList = dependencyModuleDescriptors
  )
}

private fun createCompilerConfiguration(
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
    put(
      CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
      PrintingMessageCollector(
        System.err,
        MessageRenderer.PLAIN_FULL_PATHS,
        false
      )
    )

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
    // addJvmClasspathRoots(classpathFiles)
  }
}

/*
Borrowed from Detekt
https://github.com/detekt/detekt/blob/master/detekt-parser/src/main/kotlin/io/github/detekt/parser/KotlinEnvironmentUtils.kt
 */
private fun createKotlinCoreEnvironment(
  configuration: CompilerConfiguration,
  disposable: Disposable = Disposer.newDisposable()
): KotlinCoreEnvironment {
  // https://github.com/JetBrains/kotlin/commit/2568804eaa2c8f6b10b735777218c81af62919c1
  setIdeaIoUseFallback()
  configuration.put(CommonConfigurationKeys.MODULE_NAME, "moduleCheck")

  val environment = KotlinCoreEnvironment.createForProduction(
    disposable,
    configuration,
    EnvironmentConfigFiles.JVM_CONFIG_FILES
  )

  val projectCandidate = environment.project

  val project = requireNotNull(projectCandidate as? MockProject) {
    "MockProject type expected, actual - ${projectCandidate.javaClass.simpleName}"
  }

  project.registerService(PomModel::class.java, ModuleCheckPomModel())

  return environment
}

/**
 * https://github.com/pinterest/ktlint/blob/69cc0f7f826e18d7ec20e7a0f05df12d53a3c1e1/ktlint-core/src/main/kotlin/com/pinterest/ktlint/core/internal/KotlinPsiFileFactory.kt#L70
 *
 * @since 0.13.0
 */
private class ModuleCheckPomModel : UserDataHolderBase(), PomModel {

  override fun runTransaction(transaction: PomTransaction) {
    val transactionCandidate = transaction as? PomTransactionBase

    val pomTransaction = requireNotNull(transactionCandidate) {
      "expected ${PomTransactionBase::class.simpleName} " +
        "but actual was ${transaction.javaClass.simpleName}"
    }

    pomTransaction.run()
  }

  override fun <T : PomModelAspect?> getModelAspect(aspect: Class<T>): T? {
    if (aspect == TreeAspect::class.java) {
      // using approach described in https://git.io/vKQTo due to the magical bytecode of TreeAspect
      // (check constructor signature and compare it to the source)
      // (org.jetbrains.kotlin:kotlin-compiler-embeddable:1.0.3)
      val constructor = ReflectionFactory.getReflectionFactory()
        .newConstructorForSerialization(aspect, Any::class.java.getDeclaredConstructor())
      @Suppress("UNCHECKED_CAST")
      return constructor.newInstance() as T
    }
    return null
  }
}
