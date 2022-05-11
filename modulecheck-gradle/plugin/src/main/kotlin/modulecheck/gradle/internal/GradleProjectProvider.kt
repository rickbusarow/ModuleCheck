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

package modulecheck.gradle.internal

import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.TestedExtension
import com.squareup.anvil.plugin.AnvilExtension
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import modulecheck.config.ModuleCheckSettings
import modulecheck.core.rule.KAPT_PLUGIN_ID
import modulecheck.gradle.GradleMcLogger
import modulecheck.gradle.platforms.AndroidPlatformPluginFactory
import modulecheck.gradle.platforms.JvmPlatformPluginFactory
import modulecheck.gradle.platforms.internal.toJavaVersion
import modulecheck.parsing.gradle.dsl.BuildFileParser
import modulecheck.parsing.gradle.model.ProjectPath.StringProjectPath
import modulecheck.parsing.gradle.model.asConfigurationName
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
import modulecheck.project.impl.RealMcProject
import net.swiftzer.semver.SemVer
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.internal.component.external.model.ProjectDerivedCapability
import org.gradle.api.Project as GradleProject

@Suppress("LongParameterList")
class GradleProjectProvider @AssistedInject constructor(
  @Assisted
  private val rootGradleProject: GradleProject,
  private val settings: ModuleCheckSettings,
  override val projectCache: ProjectCache,
  private val gradleLogger: GradleMcLogger,
  private val buildFileParserFactory: BuildFileParser.Factory,
  private val jvmFileProviderFactory: RealJvmFileProvider.Factory,
  private val androidPlatformPluginFactory: AndroidPlatformPluginFactory,
  private val jvmPlatformPluginFactory: JvmPlatformPluginFactory
) : ProjectProvider {

  private val gradleProjects = rootGradleProject.allprojects
    .associateBy { StringProjectPath(it.path) }

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

    val projectDependencies = gradleProject.projectDependencies()
    val externalDependencies = gradleProject.externalDependencies()

    val hasKapt = gradleProject
      .pluginManager
      .hasPlugin(KAPT_PLUGIN_ID)

    val androidCommonExtensionOrNull = gradleProject
      .extensions
      .findByType(CommonExtension::class.java)

    val hasTestFixturesPlugin = gradleProject
      .pluginManager
      .hasPlugin(TEST_FIXTURES_PLUGIN_ID) || (androidCommonExtensionOrNull as? TestedExtension)
      ?.testFixtures?.enable == true

    val platformPlugin = if (androidCommonExtensionOrNull != null) {

      androidPlatformPluginFactory.create(
        gradleProject = gradleProject,
        androidCommonExtension = androidCommonExtensionOrNull,
        hasTestFixturesPlugin = hasTestFixturesPlugin
      )
    } else {
      jvmPlatformPluginFactory.create(
        gradleProject = gradleProject,
        hasTestFixturesPlugin = hasTestFixturesPlugin
      )
    }

    return RealMcProject(
      path = path,
      projectDir = gradleProject.projectDir,
      buildFile = gradleProject.buildFile,
      hasKapt = hasKapt,
      hasTestFixturesPlugin = hasTestFixturesPlugin,
      projectCache = projectCache,
      anvilGradlePlugin = gradleProject.anvilGradlePluginOrNull(),
      logger = gradleLogger,
      jvmFileProviderFactory = jvmFileProviderFactory,
      javaSourceVersion = gradleProject.javaVersion(),
      projectDependencies = projectDependencies,
      externalDependencies = externalDependencies,
      buildFileParserFactory = buildFileParserFactory,
      platformPlugin = platformPlugin
    )
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

  @AssistedFactory
  fun interface Factory {
    fun create(rootGradleProject: GradleProject): GradleProjectProvider
  }

  companion object {
    private const val TEST_FIXTURES_SUFFIX = "-test-fixtures"
    private const val TEST_FIXTURES_PLUGIN_ID = "java-test-fixtures"
  }
}
