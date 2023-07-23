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

import modulecheck.gradle.GradleMcLogger
import modulecheck.gradle.platforms.android.AgpApiAccess
import modulecheck.gradle.platforms.android.AndroidPlatformPluginFactory
import modulecheck.gradle.platforms.internal.GradleProject
import modulecheck.gradle.platforms.jvm.JvmPlatformPluginFactory
import modulecheck.gradle.platforms.kotlin.jvmTarget
import modulecheck.model.dependency.ProjectPath
import modulecheck.parsing.gradle.dsl.BuildFileParser
import modulecheck.parsing.source.AnvilGradlePlugin
import modulecheck.parsing.wiring.RealJvmFileProvider
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.project.impl.RealMcProject
import modulecheck.rule.impl.KAPT_PLUGIN_ID
import javax.inject.Inject

class ProjectCacheFactory @Inject constructor(
  private val projectCache: ProjectCache,
  private val gradleLogger: GradleMcLogger,
  private val agpApiAccess: AgpApiAccess,
  private val buildFileParserFactory: BuildFileParser.Factory,
  private val jvmFileProviderFactory: RealJvmFileProvider.Factory,
  private val androidPlatformPluginFactory: AndroidPlatformPluginFactory,
  private val jvmPlatformPluginFactory: JvmPlatformPluginFactory
) {
  fun create(rootGradleProject: GradleProject): GradleProjectProvider {
    rootGradleProject.allprojects
      .filter { it.buildFile.exists() }
      .forEach { sub ->
        val subPath = ProjectPath.StringProjectPath(sub.path)

        projectCache[subPath] = createProject(
          gradleProject = sub,
          path = subPath,
          projectCache = projectCache
        )
      }

    return GradleProjectProvider(projectCache = projectCache)
  }

  private fun createProject(
    gradleProject: GradleProject,
    path: ProjectPath.StringProjectPath,
    projectCache: ProjectCache
  ): McProject {

    val hasKapt = gradleProject.pluginManager.hasPlugin(KAPT_PLUGIN_ID)

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
      projectPath = path,
      projectDir = gradleProject.projectDir,
      buildFile = gradleProject.buildFile,
      hasKapt = hasKapt,
      hasTestFixturesPlugin = hasTestFixturesPlugin,
      projectCache = projectCache,
      anvilGradlePlugin = gradleProject.anvilGradlePluginOrNull(),
      logger = gradleLogger,
      jvmFileProviderFactory = jvmFileProviderFactory,
      jvmTarget = gradleProject.jvmTarget(),
      buildFileParserFactory = buildFileParserFactory,
      platformPlugin = platformPlugin
    )
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
      ?.let { versionString -> net.swiftzer.semver.SemVer.parse(versionString) }
      ?: return null

    // TODO This is something of a band-aid, because it means that if the plugin is only applied to
    //  a sub-project, it won't be parsed at all.  Anvil should probably be shaded, unless/until
    //  classpath isolation with the Worker API is working.
    @Suppress("SwallowedException")
    val enabled = try {
      extensions.findByType(com.squareup.anvil.plugin.AnvilExtension::class.java)
        ?.generateDaggerFactories
        ?.get() == true
    } catch (e: NoClassDefFoundError) {
      return null
    }

    return AnvilGradlePlugin(version, enabled)
  }

  companion object {
    private const val TEST_FIXTURES_PLUGIN_ID = "java-test-fixtures"
  }
}
