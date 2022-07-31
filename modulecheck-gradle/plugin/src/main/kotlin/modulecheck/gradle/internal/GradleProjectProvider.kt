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

package modulecheck.gradle.internal

import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.anvil.plugin.AnvilExtension
import modulecheck.config.ModuleCheckSettings
import modulecheck.dagger.AppScope
import modulecheck.dagger.RootGradleProject
import modulecheck.gradle.GradleMcLogger
import modulecheck.gradle.platforms.JvmPlatformPluginFactory
import modulecheck.gradle.platforms.android.AgpApiAccess
import modulecheck.gradle.platforms.android.AndroidPlatformPluginFactory
import modulecheck.gradle.platforms.sourcesets.jvmTarget
import modulecheck.model.dependency.AllProjectPathsProvider
import modulecheck.model.dependency.ExternalDependencies
import modulecheck.model.dependency.ExternalDependency
import modulecheck.model.dependency.ProjectDependencies
import modulecheck.model.dependency.ProjectDependency
import modulecheck.model.dependency.ProjectPath
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.model.dependency.ProjectPath.TypeSafeProjectPath
import modulecheck.model.dependency.TypeSafeProjectPathResolver
import modulecheck.model.dependency.asConfigurationName
import modulecheck.parsing.gradle.dsl.BuildFileParser
import modulecheck.parsing.gradle.model.GradleProject
import modulecheck.parsing.gradle.model.GradleProjectDependency
import modulecheck.parsing.source.AnvilGradlePlugin
import modulecheck.parsing.wiring.RealJvmFileProvider
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.project.ProjectProvider
import modulecheck.project.impl.RealMcProject
import modulecheck.rule.impl.KAPT_PLUGIN_ID
import modulecheck.utils.mapToSet
import net.swiftzer.semver.SemVer
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.internal.component.external.model.ProjectDerivedCapability
import javax.inject.Inject

@Suppress("LongParameterList")
@ContributesBinding(AppScope::class, ProjectProvider::class)
class GradleProjectProvider @Inject constructor(
  @RootGradleProject
  private val rootGradleProject: GradleProject,
  private val settings: ModuleCheckSettings,
  override val projectCache: ProjectCache,
  private val gradleLogger: GradleMcLogger,
  private val agpApiAccess: AgpApiAccess,
  private val buildFileParserFactory: BuildFileParser.Factory,
  private val jvmFileProviderFactory: RealJvmFileProvider.Factory,
  private val androidPlatformPluginFactory: AndroidPlatformPluginFactory,
  private val jvmPlatformPluginFactory: JvmPlatformPluginFactory,
  private val typeSafeProjectPathResolver: TypeSafeProjectPathResolver,
  private val allProjectPathsProviderDelegate: AllProjectPathsProvider,
  private val projectDependency: ProjectDependency.Factory,
  private val externalDependency: ExternalDependency.Factory
) : ProjectProvider, AllProjectPathsProvider by allProjectPathsProviderDelegate {

  private val gradleProjects = rootGradleProject.allprojects
    .associateBy { StringProjectPath(it.path) }

  override fun get(path: ProjectPath): McProject {
    return projectCache.getOrPut(path) {

      when (path) {
        is StringProjectPath -> createProject(path)
        is TypeSafeProjectPath -> createProject(
          typeSafeProjectPathResolver.resolveStringProjectPath(path)
        )
      }
    }
  }

  override fun getAll(): List<McProject> {
    return rootGradleProject.allprojects
      .filter { it.buildFile.exists() }
      .filterNot { it.path in settings.doNotCheck }
      .map { get(StringProjectPath(it.path)) }
  }

  override fun getAllPaths(): List<StringProjectPath> {
    return allProjectPathsProviderDelegate.getAllPaths()
  }

  override fun clearCaches() {
    projectCache.clearContexts()
  }

  private fun createProject(path: StringProjectPath): McProject {
    val gradleProject = gradleProjects.getValue(path)

    val projectDependencies = gradleProject.projectDependencies()
    val externalDependencies = gradleProject.externalDependencies()

    val hasKapt = gradleProject
      .pluginManager
      .hasPlugin(KAPT_PLUGIN_ID)

    val hasTestFixturesPlugin = gradleProject
      .pluginManager
      .hasPlugin(TEST_FIXTURES_PLUGIN_ID)

    val platformPlugin = agpApiAccess.ifSafeOrNull(gradleProject) {

      androidPlatformPluginFactory.create(
        hasTestFixturesPlugin = hasTestFixturesPlugin
      )
    } ?: jvmPlatformPluginFactory.create(
      gradleProject = gradleProject,
      hasTestFixturesPlugin = hasTestFixturesPlugin
    )

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
      jvmTarget = gradleProject.jvmTarget(),
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
          .mapToSet { dep ->

            externalDependency.create(
              configurationName = configuration.name.asConfigurationName(),
              group = dep.group,
              moduleName = dep.name,
              version = dep.version,
              isTestFixture = dep.isTestFixtures()
            )
          }

        configuration.name.asConfigurationName() to externalDependencies
      }
      .toMutableMap()

    ExternalDependencies(map)
  }

  private fun ModuleDependency.isTestFixtures() = requestedCapabilities
    .filterIsInstance<ProjectDerivedCapability>()
    .any { capability -> capability.capabilityId.endsWith(TEST_FIXTURES_SUFFIX) }

  private fun GradleProject.projectDependencies(): Lazy<ProjectDependencies> = lazy {
    val map = configurations
      .filterNot { it.name == "ktlintRuleset" }
      .associate { config ->
        config.name.asConfigurationName() to config.dependencies
          .withType(GradleProjectDependency::class.java)
          .mapToSet {

            projectDependency.create(
              configurationName = config.name.asConfigurationName(),
              path = StringProjectPath(it.dependencyProject.path),
              isTestFixture = it.isTestFixtures()
            )
          }
      }
      .toMutableMap()
    ProjectDependencies(map)
  }

  private fun GradleProject.anvilGradlePluginOrNull(): AnvilGradlePlugin? {
    /*
    Before Kotlin 1.5.0, Anvil was applied to the `kotlinCompilerPluginClasspath` config.

    In 1.5.0+, it's applied to individual source sets, such as
    `kotlinCompilerPluginClasspathMain`, `kotlinCompilerPluginClasspathTest`, etc.
     */
    val version = configurations
      .asSequence()
      .filter { it.name.startsWith("kotlinCompilerPluginClasspath") }
      .flatMap { it.dependencies }
      .firstOrNull { it.group == "com.squareup.anvil" }
      ?.version
      ?.let { versionString -> SemVer.parse(versionString) }
      ?: return null

    // TODO This is something of a band-aid, because it means that if the plugin is only applied to
    //  a sub-project, it won't be parsed at all.  Anvil should probably be shaded, unless/until
    //  classpath isolation with the Worker API is working.
    @Suppress("SwallowedException")
    val enabled = try {
      extensions.findByType(AnvilExtension::class.java)
        ?.generateDaggerFactories
        ?.get() == true
    } catch (e: NoClassDefFoundError) {
      return null
    }

    return AnvilGradlePlugin(version, enabled)
  }

  companion object {
    private const val TEST_FIXTURES_SUFFIX = "-test-fixtures"
    private const val TEST_FIXTURES_PLUGIN_ID = "java-test-fixtures"
  }
}
