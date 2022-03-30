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

package modulecheck.project.test

import io.kotest.assertions.fail
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import modulecheck.api.context.classpathDependencies
import modulecheck.api.context.declarations
import modulecheck.api.context.references
import modulecheck.parsing.gradle.AndroidPlatformPlugin.AndroidApplicationPlugin
import modulecheck.parsing.gradle.AndroidPlatformPlugin.AndroidDynamicFeaturePlugin
import modulecheck.parsing.gradle.AndroidPlatformPlugin.AndroidLibraryPlugin
import modulecheck.parsing.gradle.AndroidPlatformPlugin.AndroidTestPlugin
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.JvmPlatformPlugin.JavaLibraryPlugin
import modulecheck.parsing.gradle.JvmPlatformPlugin.KotlinJvmPlugin
import modulecheck.parsing.gradle.PlatformPlugin
import modulecheck.parsing.gradle.ProjectPath.StringProjectPath
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.source.Reference.ExplicitReference
import modulecheck.parsing.source.Reference.InterpretedReference
import modulecheck.parsing.source.Reference.UnqualifiedAndroidResourceReference
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.project.ProjectProvider
import modulecheck.testing.BaseTest
import modulecheck.utils.lazySet
import java.io.File
import java.nio.charset.Charset

abstract class ProjectTest : BaseTest() {

  val projectCache: ProjectCache by resets { ProjectCache() }

  val projectProvider: ProjectProvider by resets {
    object : ProjectProvider {

      override val projectCache: ProjectCache
        get() = this@ProjectTest.projectCache

      override fun get(path: StringProjectPath): McProject {
        return projectCache.getValue(path)
      }

      override fun getAll(): List<McProject> = allProjects()

      override fun clearCaches() {
        allProjects().forEach { it.clearContext() }
      }
    }
  }

  fun PlatformPlugin.toBuilder(): PlatformPluginBuilder<*> {

    return when (this) {
      is AndroidApplicationPlugin -> AndroidApplicationPluginBuilder(
        viewBindingEnabled = viewBindingEnabled,
        kotlinAndroidExtensionEnabled = kotlinAndroidExtensionEnabled,
        manifests = manifests.toMutableMap(),
        sourceSets = sourceSets.toBuilderMap(),
        configurations = configurations.toBuilderMap()
      )
      is AndroidDynamicFeaturePlugin -> AndroidDynamicFeaturePluginBuilder(
        viewBindingEnabled = viewBindingEnabled,
        kotlinAndroidExtensionEnabled = kotlinAndroidExtensionEnabled,
        buildConfigEnabled = buildConfigEnabled,
        manifests = manifests.toMutableMap(),
        sourceSets = sourceSets.toBuilderMap(),
        configurations = configurations.toBuilderMap()
      )
      is AndroidLibraryPlugin -> AndroidLibraryPluginBuilder(
        viewBindingEnabled = viewBindingEnabled,
        kotlinAndroidExtensionEnabled = kotlinAndroidExtensionEnabled,
        buildConfigEnabled = buildConfigEnabled,
        androidResourcesEnabled = androidResourcesEnabled,
        manifests = manifests.toMutableMap(),
        sourceSets = sourceSets.toBuilderMap(),
        configurations = configurations.toBuilderMap()
      )
      is AndroidTestPlugin -> AndroidTestPluginBuilder(
        viewBindingEnabled = viewBindingEnabled,
        kotlinAndroidExtensionEnabled = kotlinAndroidExtensionEnabled,
        buildConfigEnabled = buildConfigEnabled,
        manifests = manifests.toMutableMap(),
        sourceSets = sourceSets.toBuilderMap(),
        configurations = configurations.toBuilderMap()
      )
      is JavaLibraryPlugin -> JavaLibraryPluginBuilder(
        sourceSets = sourceSets.toBuilderMap(),
        configurations = configurations.toBuilderMap()
      )
      is KotlinJvmPlugin -> KotlinJvmPluginBuilder(
        sourceSets = sourceSets.toBuilderMap(),
        configurations = configurations.toBuilderMap()
      )
    }
  }

  inline fun <reified P : PlatformPluginBuilder<*>> McProject.toProjectBuilder():
    McProjectBuilder<P> {

    return McProjectBuilder(
      path = path,
      projectDir = projectDir,
      buildFile = buildFile,
      projectDependencies = projectDependencies,
      externalDependencies = externalDependencies,
      hasKapt = hasKapt,
      anvilGradlePlugin = anvilGradlePlugin,
      projectCache = projectCache,
      hasTestFixturesPlugin = hasTestFixturesPlugin,
      javaSourceVersion = javaSourceVersion,
      platformPlugin = platformPlugin.toBuilder() as P
    )
  }

  inline fun <
    reified T : McProjectBuilder<P>,
    reified P : PlatformPluginBuilder<G>,
    G : PlatformPlugin> McProject.edit(
    config: McProjectBuilder<P>.() -> Unit = {}
  ): McProject {

    return toProjectBuilder<P>()
      .also { it.config() }
      .toRealMcProject()
  }

  fun kotlinProject(
    path: String,
    config: McProjectBuilder<KotlinJvmPluginBuilder>.() -> Unit = {}
  ): McProject {

    val platformPlugin = KotlinJvmPluginBuilder()

    return createProject(
      projectCache = projectCache,
      projectDir = testProjectDir,
      path = path,
      pluginBuilder = platformPlugin,
      androidPackageOrNull = null,
      config = config
    )
  }

  fun androidApplication(
    path: String,
    androidPackage: String,
    config: McProjectBuilder<AndroidApplicationPluginBuilder>.() -> Unit = {}
  ): McProject {

    return createProject(
      projectCache = projectCache,
      projectDir = testProjectDir,
      path = path,
      pluginBuilder = AndroidApplicationPluginBuilder(),
      androidPackageOrNull = androidPackage,
      config = config
    )
  }

  fun androidLibrary(
    path: String,
    androidPackage: String,
    config: McProjectBuilder<AndroidLibraryPluginBuilder>.() -> Unit = {}
  ): McProject {

    return createProject(
      projectCache = projectCache,
      projectDir = testProjectDir,
      path = path,
      pluginBuilder = AndroidLibraryPluginBuilder(),
      androidPackageOrNull = androidPackage,
      config = config
    )
  }

  fun androidDynamicFeature(
    path: String,
    androidPackage: String,
    config: McProjectBuilder<AndroidDynamicFeaturePluginBuilder>.() -> Unit = {}
  ): McProject {

    return createProject(
      projectCache = projectCache,
      projectDir = testProjectDir,
      path = path,
      pluginBuilder = AndroidDynamicFeaturePluginBuilder(),
      androidPackageOrNull = androidPackage,
      config = config
    )
  }

  fun androidTest(
    path: String,
    androidPackage: String,
    config: McProjectBuilder<AndroidTestPluginBuilder>.() -> Unit = {}
  ): McProject {

    return createProject(
      projectCache = projectCache,
      projectDir = testProjectDir,
      path = path,
      pluginBuilder = AndroidTestPluginBuilder(),
      androidPackageOrNull = androidPackage,
      config = config
    )
  }

  fun McProject.addDependency(
    configurationName: ConfigurationName,
    project: McProject,
    asTestFixture: Boolean = false
  ) {

    val old = projectDependencies[configurationName].orEmpty()

    val cpd = ConfiguredProjectDependency(configurationName, project, asTestFixture)

    projectDependencies[configurationName] = old + cpd
  }

  fun simpleProject(
    buildFileText: String? = null,
    path: String = ":lib"
  ) = this.kotlinProject(path) {

    if (buildFileText != null) {
      buildFile.writeText(buildFileText)
    }

    addSource(
      "com/lib1/Lib1Class.kt",
      """
      package com.lib1

      class Lib1Class
      """,
      SourceSetName.MAIN
    )
  }

  fun allProjects(): List<McProject> = projectCache.values.toList()

  fun File.writeText(content: String) {
    writeText(content.trimIndent(), Charset.defaultCharset())
  }

  suspend fun resolveReferences() {

    projectCache.values
      .forEach { project ->

        val thisProjectDeclarations = project.declarations().all()

        val allDependencies = project.classpathDependencies().all().map { it.contributed }
          .plus(project.projectDependencies.values.flatten())
          .map { dependency -> dependency.declarations() }
          .plus(thisProjectDeclarations)
          .let { lazySet(it) }
          .map { it.fqName }
          .toSet()

        project.references().all()
          .toList()
          .forEach eachRef@{ reference ->

            val referenceName = when (reference) {
              is ExplicitReference -> reference.fqName
              is InterpretedReference -> return@eachRef
              is UnqualifiedAndroidResourceReference -> reference.fqName
            }

            // Only check for references which would be provided by internal projects. Using a
            // block-list is a bit of a hack, but it's safer to have to add than remove.
            if (referenceName.startsWith("androidx")) return@eachRef

            val unresolved = !allDependencies.contains(referenceName)

            if (unresolved) {
              fail(
                """
                |Project ${project.path} has a reference which must be declared in a dependency kotlinProject.
                |
                |-- reference:
                |   $referenceName
                |
                |-- all declarations:
                |${allDependencies.joinToString("\n") { "   $it" }}
                |
                |-- all dependencies:
                |${project.projectDependencies.values.flatten().joinToString("\n") { "   $it" }}
                |
                |_________
                """.trimMargin()

              )
            }
          }
      }
  }
}
