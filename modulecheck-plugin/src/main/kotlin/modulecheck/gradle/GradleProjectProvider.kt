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

package modulecheck.gradle

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestedExtension
import com.squareup.anvil.plugin.AnvilExtension
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.core.rule.KAPT_PLUGIN_ID
import modulecheck.core.rule.KOTLIN_ANDROID_EXTENSIONS_PLUGIN_ID
import modulecheck.gradle.internal.androidManifests
import modulecheck.gradle.internal.sourcesets.AndroidSourceSetsParser
import modulecheck.gradle.internal.sourcesets.JvmSourceSetParser
import modulecheck.gradle.task.GradleLogger
import modulecheck.parsing.gradle.BuildFileParser
import modulecheck.parsing.gradle.ConfigFactory
import modulecheck.parsing.gradle.Configurations
import modulecheck.parsing.gradle.ProjectPath.StringProjectPath
import modulecheck.parsing.gradle.SourceSets
import modulecheck.parsing.gradle.asConfigurationName
import modulecheck.parsing.source.AnvilGradlePlugin
import modulecheck.parsing.source.JavaVersion
import modulecheck.parsing.wiring.RealJvmFileProvider
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.ExternalDependencies
import modulecheck.project.ExternalDependency
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.project.ProjectDependencies
import modulecheck.project.ProjectProvider
import modulecheck.project.impl.RealAndroidMcProject
import modulecheck.project.impl.RealMcProject
import modulecheck.utils.unsafeLazy
import net.swiftzer.semver.SemVer
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.initialization.dsl.ScriptHandler
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.internal.component.external.model.ProjectDerivedCapability

class GradleProjectProvider @AssistedInject constructor(
  @Assisted
  private val rootGradleProject: GradleProject,
  private val settings: ModuleCheckSettings,
  override val projectCache: ProjectCache,
  private val gradleLogger: GradleLogger,
  private val buildFileParserFactory: BuildFileParser.Factory,
  private val jvmFileProviderFactory: RealJvmFileProvider.Factory
) : ProjectProvider {

  private val gradleProjects = rootGradleProject.allprojects
    .associateBy { StringProjectPath(it.path) }

  private val configFactory = ConfigFactory<Configuration>(
    identifier = { name },
    allFactory = { rootGradleProject.configurations.asSequence() },
    extendsFrom = {
      rootGradleProject.configurations.findByName(this)?.extendsFrom?.toList()
        .orEmpty()
    }
  )

  override fun get(path: StringProjectPath): McProject {
    return projectCache.getOrPut(path) {
      createProject(path)
    }
  }

  override fun getAll(): List<McProject> {
    return rootGradleProject.allprojects
      .filter { it.buildFile.exists() }
      .filterNot { it.path in settings.doNotCheck }
      .map { get(StringProjectPath(it.path)) }
  }

  override fun clearCaches() {
    projectCache.clearContexts()
  }

  @Suppress("UnstableApiUsage")
  private fun createProject(path: StringProjectPath): McProject {
    val gradleProject = gradleProjects.getValue(path)

    val configurations = gradleProject.configurations()

    val projectDependencies = gradleProject.projectDependencies()
    val externalDependencies = gradleProject.externalDependencies()

    val hasKapt = gradleProject
      .pluginManager
      .hasPlugin(KAPT_PLUGIN_ID)

    val androidTestedExtension = gradleProject
      .extensions
      .findByType(TestedExtension::class.java)

    val isAndroid = androidTestedExtension != null

    val libraryExtension by unsafeLazy {
      androidTestedExtension as? LibraryExtension
    }

    val hasTestFixturesPlugin = gradleProject
      .pluginManager
      .hasPlugin(TEST_FIXTURES_PLUGIN_ID) || androidTestedExtension?.testFixtures?.enable == true

    val hasKotlinAndroidExtensions = gradleProject
      .pluginManager
      .hasPlugin(KOTLIN_ANDROID_EXTENSIONS_PLUGIN_ID)

    return if (isAndroid) {
      RealAndroidMcProject(
        path = path,
        projectDir = gradleProject.projectDir,
        buildFile = gradleProject.buildFile,
        configurations = configurations,
        hasKapt = hasKapt,
        hasTestFixturesPlugin = hasTestFixturesPlugin,
        sourceSets = gradleProject.androidSourceSets(configurations, hasTestFixturesPlugin),
        projectCache = projectCache,
        anvilGradlePlugin = gradleProject.anvilGradlePluginOrNull(),
        androidResourcesEnabled = libraryExtension?.buildFeatures?.androidResources != false,
        viewBindingEnabled = androidTestedExtension?.buildFeatures?.viewBinding == true,
        kotlinAndroidExtensionEnabled = hasKotlinAndroidExtensions,
        manifests = gradleProject.androidManifests().orEmpty(),
        logger = gradleLogger,
        jvmFileProviderFactory = jvmFileProviderFactory,
        javaSourceVersion = gradleProject.javaVersion(),
        projectDependencies = projectDependencies,
        externalDependencies = externalDependencies,
        buildFileParserFactory = buildFileParserFactory
      )
    } else {
      RealMcProject(
        path = path,
        projectDir = gradleProject.projectDir,
        buildFile = gradleProject.buildFile,
        configurations = configurations,
        hasKapt = hasKapt,
        hasTestFixturesPlugin = hasTestFixturesPlugin,
        sourceSets = gradleProject.jvmSourceSets(configurations),
        projectCache = projectCache,
        anvilGradlePlugin = gradleProject.anvilGradlePluginOrNull(),
        logger = gradleLogger,
        jvmFileProviderFactory = jvmFileProviderFactory,
        javaSourceVersion = gradleProject.javaVersion(),
        projectDependencies = projectDependencies,
        externalDependencies = externalDependencies,
        buildFileParserFactory = buildFileParserFactory
      )
    }
  }

  private fun GradleProject.configurations(): Configurations {

    val map = configurations
      .filterNot { it.name == ScriptHandler.CLASSPATH_CONFIGURATION }
      .associate { configuration ->

        val config = configFactory.create(configuration)

        configuration.name.asConfigurationName() to config
      }
    return Configurations(map)
  }

  private fun GradleProject.externalDependencies(): Lazy<ExternalDependencies> = lazy {
    val map = configurations
      .associate { configuration ->

        val externalDependencies = configuration.dependencies
          .filterIsInstance<ExternalModuleDependency>()
          .map { dep ->

            ExternalDependency(
              configurationName = configuration.name.asConfigurationName(),
              group = dep.group,
              moduleName = dep.name,
              version = dep.version
            )
          }

        configuration.name.asConfigurationName() to externalDependencies
      }
      .toMutableMap()

    ExternalDependencies(map)
  }

  private fun GradleProject.projectDependencies(): Lazy<ProjectDependencies> = lazy {
    val map = configurations
      .filterNot { it.name == "ktlintRuleset" }
      .associate { config ->
        config.name.asConfigurationName() to config.dependencies
          .withType(ProjectDependency::class.java)
          .map {

            val isTestFixture = it.requestedCapabilities
              .filterIsInstance<ProjectDerivedCapability>()
              .any { capability -> capability.capabilityId.endsWith(TEST_FIXTURES_SUFFIX) }

            ConfiguredProjectDependency(
              configurationName = config.name.asConfigurationName(),
              project = get(StringProjectPath(it.dependencyProject.path)),
              isTestFixture = isTestFixture
            )
          }
      }
      .toMutableMap()
    ProjectDependencies(map)
  }

  private fun GradleProject.javaVersion(): JavaVersion {
    return extensions.findByType(JavaPluginExtension::class.java)
      ?.sourceCompatibility
      ?.toJavaVersion()
      ?: JavaVersion.VERSION_1_8
  }

  private fun GradleProject.anvilGradlePluginOrNull(): AnvilGradlePlugin? {
    /*
    Before Kotlin 1.5.0, Anvil was applied to the `kotlinCompilerPluginClasspath` config.

    In 1.5.0+, it's applied to individual source sets, such as
    `kotlinCompilerPluginClasspathMain`, `kotlinCompilerPluginClasspathTest`, etc.
     */
    val version = configurations
      .filter { it.name.startsWith("kotlinCompilerPluginClasspath") }
      .asSequence()
      .flatMap { it.dependencies }
      .firstOrNull { it.group == "com.squareup.anvil" }
      ?.version
      ?.let { versionString -> SemVer.parse(versionString) }
      ?: return null

    val enabled = extensions
      .findByType(AnvilExtension::class.java)
      ?.generateDaggerFactories
      ?.get() == true

    return AnvilGradlePlugin(version, enabled)
  }

  @Suppress("UnstableApiUsage")
  private fun GradleProject.androidSourceSets(
    mcConfigurations: Configurations,
    hasTestFixturesPlugin: Boolean
  ): SourceSets {

    return extensions.getByType(TestedExtension::class.java)
      .let { extension ->

        AndroidSourceSetsParser.parse(
          mcConfigurations, extension, hasTestFixturesPlugin
        )
      }
  }

  private fun GradleProject.jvmSourceSets(
    mcConfigurations: Configurations
  ): SourceSets {

    return JvmSourceSetParser.parse(
      parsedConfigurations = mcConfigurations,
      gradleProject = this
    )
  }

  companion object {
    private const val TEST_FIXTURES_SUFFIX = "-test-fixtures"
    private const val TEST_FIXTURES_PLUGIN_ID = "java-test-fixtures"
  }

  @AssistedFactory
  interface Factory {
    fun create(rootGradleProject: GradleProject): GradleProjectProvider
  }
}
