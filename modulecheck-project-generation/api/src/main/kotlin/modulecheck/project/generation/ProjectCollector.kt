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

package modulecheck.project.generation

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
import modulecheck.parsing.kotlin.compiler.impl.DependencyModuleDescriptorAccess
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.project.ProjectProvider
import modulecheck.utils.lazy.lazySet
import org.jetbrains.annotations.Contract
import org.jetbrains.kotlin.config.JvmTarget
import java.io.File

/**
 * Collects all projects within the scope of analysis.
 *
 * It provides the ability to create, edit and resolve references for
 * different types of projects, including Java, Kotlin, Android Library,
 * Android Application, Android Dynamic Feature, and Android Test.
 */
interface ProjectCollector {

  /** Root directory of the project hierarchy. */
  val root: File

  /** Cache of the projects. */
  val projectCache: ProjectCache

  /** Provides access to dependency module descriptors. */
  val dependencyModuleDescriptorAccess: DependencyModuleDescriptorAccess

  /** List of bindings for code generation. */
  val codeGeneratorBindings: List<CodeGeneratorBinding>

  /** Provides projects from the cache. */
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

  /**
   * Fetches all projects from the project cache.
   *
   * @return List of all projects.
   */
  fun allProjects(): List<McProject> = projectCache.values.toList()

  /**
   * Transforms a platform plugin to its builder form.
   *
   * @return Instance of the [PlatformPluginBuilder] corresponding to the plugin type.
   */
  suspend fun PlatformPlugin.toBuilder(): PlatformPluginBuilder<*> {
    return when (this) {
      is AndroidApplicationPlugin -> AndroidApplicationPluginBuilder(
        viewBindingEnabled = viewBindingEnabled,
        kotlinAndroidExtensionEnabled = kotlinAndroidExtensionEnabled,
        manifests = manifests.toMutableMap(),
        namespaces = namespaces.toMutableMap(),
        sourceSets = sourceSets.toBuilderMap(),
        configurations = configurations.toBuilderMap()
      )

      is AndroidDynamicFeaturePlugin -> AndroidDynamicFeaturePluginBuilder(
        viewBindingEnabled = viewBindingEnabled,
        kotlinAndroidExtensionEnabled = kotlinAndroidExtensionEnabled,
        buildConfigEnabled = buildConfigEnabled,
        manifests = manifests.toMutableMap(),
        namespaces = namespaces.toMutableMap(),
        sourceSets = sourceSets.toBuilderMap(),
        configurations = configurations.toBuilderMap()
      )

      is AndroidLibraryPlugin -> AndroidLibraryPluginBuilder(
        viewBindingEnabled = viewBindingEnabled,
        kotlinAndroidExtensionEnabled = kotlinAndroidExtensionEnabled,
        buildConfigEnabled = buildConfigEnabled,
        androidResourcesEnabled = androidResourcesEnabled,
        manifests = manifests.toMutableMap(),
        namespaces = namespaces.toMutableMap(),
        sourceSets = sourceSets.toBuilderMap(),
        configurations = configurations.toBuilderMap()
      )

      is AndroidTestPlugin -> AndroidTestPluginBuilder(
        viewBindingEnabled = viewBindingEnabled,
        kotlinAndroidExtensionEnabled = kotlinAndroidExtensionEnabled,
        buildConfigEnabled = buildConfigEnabled,
        manifests = manifests.toMutableMap(),
        namespaces = namespaces.toMutableMap(),
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

  /**
   * Transforms a [McProject] to a builder form that can be further configured.
   *
   * @return Instance of [McProjectBuilder] for the project.
   */
  suspend fun <P : PlatformPluginBuilder<*>> McProject.toProjectBuilder(): McProjectBuilder<P> {
    @Suppress("UNCHECKED_CAST")
    return McProjectBuilder(
      path = projectPath,
      projectDir = projectDir,
      buildFile = buildFile,
      platformPlugin = platformPlugin.toBuilder() as P,
      codeGeneratorBindings = codeGeneratorBindings,
      projectProvider = projectProvider,
      projectCache = projectCache,
      dependencyModuleDescriptorAccess = dependencyModuleDescriptorAccess,
      projectDependencies = projectDependencies,
      externalDependencies = externalDependencies,
      hasKapt = hasKapt,
      hasTestFixturesPlugin = hasTestFixturesPlugin,
      anvilGradlePlugin = anvilGradlePlugin,
      jvmTarget = sourceSets.values.firstOrNull()?.jvmTarget ?: JvmTarget.JVM_11
    )
  }

  /**
   * Edits a project with a provided configuration.
   *
   * @param config Configuration to be applied on the project builder.
   * @return Updated instance of the project.
   */
  @Contract(pure = true, value = "_->new")
  suspend fun McProject.editSimple(
    config: McProjectBuilder<PlatformPluginBuilder<PlatformPlugin>>.() -> Unit = {}
  ): McProject {
    return toProjectBuilder<PlatformPluginBuilder<PlatformPlugin>>()
      .also { it.config() }
      .toRealMcProject()
  }

  /**
   * Creates a new Java project.
   *
   * @param path Project path.
   * @param config Configuration to be applied on the project builder.
   * @return Instance of the newly created Java project.
   */
  fun javaProject(
    path: String,
    config: McProjectBuilder<JavaLibraryPluginBuilder>.() -> Unit = {}
  ): McProject {
    val platformPlugin = JavaLibraryPluginBuilder()

    return createProject(
      projectCache = projectCache,
      dependencyModuleDescriptorAccess = dependencyModuleDescriptorAccess,
      projectDir = root,
      path = path,
      pluginBuilder = platformPlugin,
      androidPackageOrNull = null,
      codeGeneratorBindings = codeGeneratorBindings,
      projectProvider = projectProvider,
      config = config
    )
  }

  /**
   * Constructs a Kotlin-based project with the provided configuration.
   *
   * @param path the relative path of the project
   * @param config an optional configuration block for the project builder
   * @return an instance of [McProject] representing the newly created Kotlin project
   */
  fun kotlinProject(
    path: String,
    config: McProjectBuilder<KotlinJvmPluginBuilder>.() -> Unit = {}
  ): McProject {
    val platformPlugin = KotlinJvmPluginBuilder()

    return createProject(
      projectCache = projectCache,
      dependencyModuleDescriptorAccess = dependencyModuleDescriptorAccess,
      projectDir = root,
      path = path,
      pluginBuilder = platformPlugin,
      androidPackageOrNull = null,
      codeGeneratorBindings = codeGeneratorBindings,
      projectProvider = projectProvider,
      config = config
    )
  }

  /**
   * Constructs an Android application project with the provided configuration.
   *
   * @param path the relative path of the project
   * @param androidPackage the Android package name for the project
   * @param config an optional configuration block for the project builder
   * @return an instance of [McProject] representing the newly created Android application project
   */
  fun androidApplication(
    path: String,
    androidPackage: String,
    config: McProjectBuilder<AndroidApplicationPluginBuilder>.() -> Unit = {}
  ): McProject {
    return createProject(
      projectCache = projectCache,
      dependencyModuleDescriptorAccess = dependencyModuleDescriptorAccess,
      projectDir = root,
      path = path,
      pluginBuilder = AndroidApplicationPluginBuilder(),
      androidPackageOrNull = androidPackage,
      codeGeneratorBindings = codeGeneratorBindings,
      projectProvider = projectProvider,
      config = config
    )
  }

  /**
   * Constructs an Android library project with the provided configuration.
   *
   * @param path the relative path of the project
   * @param androidPackage the Android package name for the project
   * @param config an optional configuration block for the project builder
   * @return an instance of [McProject] representing the newly created Android library project
   */
  fun androidLibrary(
    path: String,
    androidPackage: String,
    config: McProjectBuilder<AndroidLibraryPluginBuilder>.() -> Unit = {}
  ): McProject {
    return createProject(
      projectCache = projectCache,
      dependencyModuleDescriptorAccess = dependencyModuleDescriptorAccess,
      projectDir = root,
      path = path,
      pluginBuilder = AndroidLibraryPluginBuilder(),
      androidPackageOrNull = androidPackage,
      codeGeneratorBindings = codeGeneratorBindings,
      projectProvider = projectProvider,
      config = config
    )
  }

  /**
   * Constructs an Android dynamic feature module with the provided configuration.
   *
   * @param path the relative path of the project
   * @param androidPackage the Android package name for the project
   * @param config an optional configuration block for the project builder
   * @return an instance of [McProject] representing
   *   the newly created Android dynamic feature module
   */
  fun androidDynamicFeature(
    path: String,
    androidPackage: String,
    config: McProjectBuilder<AndroidDynamicFeaturePluginBuilder>.() -> Unit = {}
  ): McProject {
    return createProject(
      projectCache = projectCache,
      dependencyModuleDescriptorAccess = dependencyModuleDescriptorAccess,
      projectDir = root,
      path = path,
      pluginBuilder = AndroidDynamicFeaturePluginBuilder(),
      androidPackageOrNull = androidPackage,
      codeGeneratorBindings = codeGeneratorBindings,
      projectProvider = projectProvider,
      config = config
    )
  }

  /**
   * Constructs an Android test project with the provided configuration.
   *
   * @param path the relative path of the project
   * @param androidPackage the Android package name for the project
   * @param config an optional configuration block for the project builder
   * @return an instance of [McProject] representing the newly created Android test project
   */
  fun androidTest(
    path: String,
    androidPackage: String,
    config: McProjectBuilder<AndroidTestPluginBuilder>.() -> Unit = {}
  ): McProject {
    return createProject(
      projectCache = projectCache,
      dependencyModuleDescriptorAccess = dependencyModuleDescriptorAccess,
      projectDir = root,
      path = path,
      pluginBuilder = AndroidTestPluginBuilder(),
      androidPackageOrNull = androidPackage,
      codeGeneratorBindings = codeGeneratorBindings,
      projectProvider = projectProvider,
      config = config
    )
  }

  /**
   * Constructs a simple Kotlin project. If the build file text is provided, it writes
   * the text to the build file. The created project will have a source file in the
   * 'main' source set with the package 'com.lib1' and a class named 'Lib1Class'.
   *
   * @param buildFileText optional text to be written into the build file
   * @param path the relative path of the project, defaults to ':lib'
   * @return an instance of [McProject] representing the newly created simple Kotlin project
   */
  fun simpleProject(buildFileText: String? = null, path: String = ":lib"): McProject =
    this.kotlinProject(path) {
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

  /**
   * Writes text to a file.
   *
   * @param text Text to be written to the file.
   */
  operator fun File.invoke(text: () -> String) {
    writeText(text().trimIndent())
  }

  /**
   * Resolves all references in the project scope, validating dependencies and declarations.
   *
   * Throws a failure if any reference is unresolved.
   */
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
                |Project ${project.projectPath} has a reference which must be declared in a dependency kotlinProject.
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
