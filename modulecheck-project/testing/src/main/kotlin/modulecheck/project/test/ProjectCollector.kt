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
import modulecheck.config.CodeGeneratorBinding
import modulecheck.model.dependency.AndroidPlatformPlugin.AndroidApplicationPlugin
import modulecheck.model.dependency.AndroidPlatformPlugin.AndroidDynamicFeaturePlugin
import modulecheck.model.dependency.AndroidPlatformPlugin.AndroidLibraryPlugin
import modulecheck.model.dependency.AndroidPlatformPlugin.AndroidTestPlugin
import modulecheck.model.dependency.JvmPlatformPlugin.JavaLibraryPlugin
import modulecheck.model.dependency.JvmPlatformPlugin.KotlinJvmPlugin
import modulecheck.model.dependency.PlatformPlugin
import modulecheck.model.dependency.ProjectPath
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.kotlin.compiler.impl.SafeAnalysisResultAccess
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.project.ProjectProvider
import modulecheck.utils.lazy.lazySet
import org.jetbrains.kotlin.config.JvmTarget
import java.io.File

interface ProjectCollector {

  val root: File
  val projectCache: ProjectCache
  val safeAnalysisResultAccess: SafeAnalysisResultAccess

  val codeGeneratorBindings: List<CodeGeneratorBinding>

  val projectProvider: ProjectProvider
    get() = object : ProjectProvider {

      override val projectCache: ProjectCache
        get() = this@ProjectCollector.projectCache

      override fun get(path: ProjectPath): McProject {
        return projectCache.getValue(path)
      }

      override fun getAll(): List<McProject> = allProjects()

      override fun clearCaches() {
        allProjects().forEach { it.clearContext() }
      }
    }

  fun allProjects(): List<McProject> = projectCache.values.toList()

  suspend fun PlatformPlugin.toBuilder(): PlatformPluginBuilder<*> {
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

  suspend fun <P : PlatformPluginBuilder<*>> McProject.toProjectBuilder(): McProjectBuilder<P> {
    @Suppress("UNCHECKED_CAST")
    return McProjectBuilder(
      path = path,
      projectDir = projectDir,
      buildFile = buildFile,
      platformPlugin = platformPlugin.toBuilder() as P,
      codeGeneratorBindings = codeGeneratorBindings,
      projectProvider = projectProvider,
      projectCache = projectCache,
      safeAnalysisResultAccess = safeAnalysisResultAccess,
      projectDependencies = projectDependencies,
      externalDependencies = externalDependencies,
      hasKapt = hasKapt,
      hasTestFixturesPlugin = hasTestFixturesPlugin,
      anvilGradlePlugin = anvilGradlePlugin,
      jvmTarget = sourceSets.values.firstOrNull()?.jvmTarget ?: JvmTarget.JVM_11
    )
  }

  suspend fun McProject.editSimple(
    config: McProjectBuilder<PlatformPluginBuilder<PlatformPlugin>>.() -> Unit = {}
  ): McProject {
    return toProjectBuilder<PlatformPluginBuilder<PlatformPlugin>>()
      .also { it.config() }
      .toRealMcProject()
  }

  fun javaProject(
    path: String,
    config: McProjectBuilder<JavaLibraryPluginBuilder>.() -> Unit = {}
  ): McProject {
    val platformPlugin = JavaLibraryPluginBuilder()

    return createProject(
      projectCache = projectCache,
      safeAnalysisResultAccess = safeAnalysisResultAccess,
      projectDir = root,
      path = path,
      pluginBuilder = platformPlugin,
      androidPackageOrNull = null,
      codeGeneratorBindings = codeGeneratorBindings,
      projectProvider = projectProvider,
      config = config
    )
  }

  fun kotlinProject(
    path: String,
    config: McProjectBuilder<KotlinJvmPluginBuilder>.() -> Unit = {}
  ): McProject {
    val platformPlugin = KotlinJvmPluginBuilder()

    return createProject(
      projectCache = projectCache,
      safeAnalysisResultAccess = safeAnalysisResultAccess,
      projectDir = root,
      path = path,
      pluginBuilder = platformPlugin,
      androidPackageOrNull = null,
      codeGeneratorBindings = codeGeneratorBindings,
      projectProvider = projectProvider,
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
      safeAnalysisResultAccess = safeAnalysisResultAccess,
      projectDir = root,
      path = path,
      pluginBuilder = AndroidApplicationPluginBuilder(),
      androidPackageOrNull = androidPackage,
      codeGeneratorBindings = codeGeneratorBindings,
      projectProvider = projectProvider,
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
      safeAnalysisResultAccess = safeAnalysisResultAccess,
      projectDir = root,
      path = path,
      pluginBuilder = AndroidLibraryPluginBuilder(),
      androidPackageOrNull = androidPackage,
      codeGeneratorBindings = codeGeneratorBindings,
      projectProvider = projectProvider,
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
      safeAnalysisResultAccess = safeAnalysisResultAccess,
      projectDir = root,
      path = path,
      pluginBuilder = AndroidDynamicFeaturePluginBuilder(),
      androidPackageOrNull = androidPackage,
      codeGeneratorBindings = codeGeneratorBindings,
      projectProvider = projectProvider,
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
      safeAnalysisResultAccess = safeAnalysisResultAccess,
      projectDir = root,
      path = path,
      pluginBuilder = AndroidTestPluginBuilder(),
      androidPackageOrNull = androidPackage,
      codeGeneratorBindings = codeGeneratorBindings,
      projectProvider = projectProvider,
      config = config
    )
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

        val allDependencies = project.classpathDependencies().all()
          .map { it.contributed }
          .plus(project.projectDependencies.values.flatten())
          .map { dependency -> dependency.declarations(projectCache) }
          .plus(thisProjectDeclarations)
          .let { lazySet(it) }
          .map { it.name }
          .toSet()

        project.references().all()
          .toList()
          .forEach eachRef@{ reference ->

            // Only check for references which would be provided by internal projects. Using a
            // block-list is a bit of a hack, but it's safer to have to add than remove.
            if (reference.name.startsWith("androidx")) return@eachRef

            val unresolved = !allDependencies.contains(reference.name)

            if (unresolved) {
              fail(
                """
                |Project ${project.path} has a reference which must be declared in a dependency kotlinProject.
                |
                |-- reference:
                |   ${reference.name}
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
