/*
 * Copyright (C) 2021-2024 Rick Busarow
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
import dispatch.core.withDefault
import modulecheck.dagger.SingleIn
import modulecheck.dagger.TaskScope
import modulecheck.gradle.platforms.ExternalDependencyDescriptorFactory
import modulecheck.model.dependency.ExternalDependencyDescriptor
import modulecheck.model.dependency.ExternalDependencyDescriptorCache
import modulecheck.model.dependency.MavenCoordinates
import modulecheck.parsing.kotlin.compiler.HasAnalysisResult
import modulecheck.reporting.logging.McLogger
import modulecheck.utils.cache.SafeCache
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.ResetManager
import modulecheck.utils.lazy.lazyDeferred
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.DeserializedDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import java.io.File
import javax.inject.Inject

@SingleIn(TaskScope::class)
@ContributesBinding(TaskScope::class)
class ExternalDependencyDescriptorCacheImpl @Inject constructor(
  private val descriptorFactory: ExternalDependencyDescriptorFactory
) : ExternalDependencyDescriptorCache {

  private val cache = SafeCache<Key, ExternalDependencyDescriptor>(
    listOf(ExternalDependencyDescriptorCacheImpl::class)
  )

  override suspend fun get(
    mavenCoordinates: MavenCoordinates,
    jarFile: File,
    kotlinLanguageVersion: LanguageVersion?,
    jvmTarget: JvmTarget
  ): ExternalDependencyDescriptor {
    return cache.getOrPut(
      Key(
        mavenCoordinates = mavenCoordinates,
        jarFile = jarFile,
        kotlinLanguageVersion = kotlinLanguageVersion,
        jvmTarget = jvmTarget
      )
    ) {
      descriptorFactory.create(
        mavenCoordinates = mavenCoordinates,
        jarFile = jarFile,
        kotlinLanguageVersion = kotlinLanguageVersion,
        jvmTarget = jvmTarget
      )
    }
  }

  private data class Key(
    val mavenCoordinates: MavenCoordinates,
    val jarFile: File,
    val kotlinLanguageVersion: LanguageVersion?,
    val jvmTarget: JvmTarget
  )
}

/**
 * @property mavenCoordinates path of the associated Gradle project
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
class ExternalDependencyDescriptorImpl(
  val mavenCoordinates: MavenCoordinates,
  val jarFile: File,
  val kotlinLanguageVersion: LanguageVersion?,
  val jvmTarget: JvmTarget,
  val logger: McLogger,
  private val resetManager: ResetManager
) : ExternalDependencyDescriptor, HasAnalysisResult {

  val compilerConfiguration = lazyDeferred {
    createCompilerConfiguration()
  }

  val coreEnvironment = lazyDeferred {
    createKotlinCoreEnvironment(compilerConfiguration.await())
  }

  override val declarations = lazyDeferred {
    moduleDescriptorDeferred.await()
      .declarations()
      .filterIsInstance<DeclarationDescriptorWithVisibility>()
      .filter { it.isEffectivelyPublicApi }
      .toSet()
  }

  val declarationsByProtoString = lazyDeferred {
    declarations.await()
      .filterIsInstance<DeserializedDescriptor>()
      .associateBy { it.protoByteString() }
  }

  override val analysisResultDeferred: LazyDeferred<AnalysisResult> = lazyDeferred {

    createAnalysisResult(
      coreEnvironment = coreEnvironment.await()
    )
  }

  override val bindingContextDeferred = lazyDeferred {
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

  override suspend fun contains(declarationDescriptor: DeclarationDescriptor): Boolean {
    val protoByteString = (declarationDescriptor as? DeserializedDescriptor)
      ?.protoByteStringOrNull()
      ?: return false

    return declarationsByProtoString.await().containsKey(protoByteString)
  }

  private fun createAnalysisResult(
    coreEnvironment: KotlinCoreEnvironment
  ): AnalysisResult {

    val analyzer = AnalyzerWithCompilerReport(
      messageCollector = messageCollector,
      languageVersionSettings = coreEnvironment.configuration.languageVersionSettings,
      renderDiagnosticName = false
    )

    analyzer.analyzeAndReport(listOf()) {
      TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
        project = coreEnvironment.project,
        files = listOf(),
        trace = NoScopeRecordCliBindingTrace(),
        configuration = coreEnvironment.configuration,
        packagePartProvider = coreEnvironment::createPackagePartProvider,
        declarationProviderFactory = ::FileBasedDeclarationProviderFactory,
        explicitModuleDependencyList = listOf()
      )
    }

    messageCollector.printIssuesCountIfAny()

    return analyzer.analysisResult
  }

  private suspend fun createCompilerConfiguration(): CompilerConfiguration = withDefault {

    CompilerConfiguration().apply {
      put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
      put(JVMConfigurationKeys.JVM_TARGET, jvmTarget)
      put(CommonConfigurationKeys.MODULE_NAME, mavenCoordinates.name)

      if (kotlinLanguageVersion != null) {
        val languageVersionSettings = LanguageVersionSettingsImpl(
          languageVersion = kotlinLanguageVersion,
          apiVersion = ApiVersion.createByLanguageVersion(kotlinLanguageVersion)
        )
        put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, languageVersionSettings)
      }

      put(JVMConfigurationKeys.JVM_TARGET, jvmTarget)

      addJvmClasspathRoots(listOf(jarFile))
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
      configFiles = EnvironmentConfigFiles.JVM_CONFIG_FILES
    )
  }

  /**
   * Dagger implementation for [ExternalDependencyDescriptorFactory]
   *
   * @since 0.13.0
   */
  @ContributesBinding(TaskScope::class)
  class Factory @Inject constructor(
    private val logger: McLogger
  ) : ExternalDependencyDescriptorFactory {
    override fun create(
      mavenCoordinates: MavenCoordinates,
      jarFile: File,
      kotlinLanguageVersion: LanguageVersion?,
      jvmTarget: JvmTarget
    ): ExternalDependencyDescriptor = ExternalDependencyDescriptorImpl(
      mavenCoordinates = mavenCoordinates,
      jarFile = jarFile,
      kotlinLanguageVersion = kotlinLanguageVersion,
      jvmTarget = jvmTarget,
      logger = logger,
      resetManager = ResetManager()
    )
  }
}
