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
import modulecheck.parsing.gradle.model.AndroidPlatformPlugin.AndroidApplicationPlugin
import modulecheck.parsing.gradle.model.AndroidPlatformPlugin.AndroidDynamicFeaturePlugin
import modulecheck.parsing.gradle.model.AndroidPlatformPlugin.AndroidLibraryPlugin
import modulecheck.parsing.gradle.model.AndroidPlatformPlugin.AndroidTestPlugin
import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.parsing.gradle.model.ConfiguredProjectDependency
import modulecheck.parsing.gradle.model.JvmPlatformPlugin.JavaLibraryPlugin
import modulecheck.parsing.gradle.model.JvmPlatformPlugin.KotlinJvmPlugin
import modulecheck.parsing.gradle.model.PlatformPlugin
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.source.Reference.ExplicitReference
import modulecheck.parsing.source.Reference.InterpretedReference
import modulecheck.parsing.source.UnqualifiedAndroidResourceReference
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.utils.lazySet
import java.io.File

interface ProjectCollector {

  val root: File
  val projectCache: ProjectCache

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

  fun <P : PlatformPluginBuilder<*>> McProject.toProjectBuilder():
    McProjectBuilder<P> {
    @Suppress("UNCHECKED_CAST")
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

  fun McProject.editSimple(
    config: McProjectBuilder<PlatformPluginBuilder<PlatformPlugin>>.() -> Unit = {}
  ): McProject {
    return toProjectBuilder<PlatformPluginBuilder<PlatformPlugin>>()
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
      projectDir = root,
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
      projectDir = root,
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
      projectDir = root,
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
      projectDir = root,
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
      projectDir = root,
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

    val cpd = ConfiguredProjectDependency(configurationName, project.path, asTestFixture)

    projectDependencies[configurationName] = old + cpd
  }

  fun simpleProject(
    buildFileText: String? = null,
    path: String = ":lib"
  ) = this.kotlinProject(path) {
    if (buildFileText != null) {
      buildFile.writeText(buildFileText)
    }

    addKotlinSource(
      """
      package com.lib1

      class Lib1Class
      """,
      SourceSetName.MAIN
    )
  }

  operator fun File.invoke(text: () -> String) {
    writeText(text().trimIndent())
  }

  suspend fun resolveReferences() {
    projectCache.values
      .forEach { project ->

        val thisProjectDeclarations = project.declarations().all()

        val allDependencies = project.classpathDependencies().all().map { it.contributed }
          .plus(project.projectDependencies.values.flatten())
          .map { dependency -> dependency.declarations(projectCache) }
          .plus(thisProjectDeclarations)
          .let { lazySet(it) }
          .map { it.name }
          .toSet()

        project.references().all()
          .toList()
          .forEach eachRef@{ reference ->

            val referenceName = when (reference) {
              is ExplicitReference -> reference.name
              is InterpretedReference -> return@eachRef
              is UnqualifiedAndroidResourceReference -> reference.name
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
