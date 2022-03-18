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
import io.kotest.common.runBlocking
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import modulecheck.api.context.androidBasePackagesForSourceSetName
import modulecheck.api.context.classpathDependencies
import modulecheck.api.context.declarations
import modulecheck.api.context.references
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.source.Reference.ExplicitReference
import modulecheck.parsing.source.Reference.InterpretedReference
import modulecheck.parsing.source.Reference.UnqualifiedAndroidResourceReference
import modulecheck.project.AndroidMcProject
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.project.ProjectProvider
import modulecheck.testing.BaseTest
import modulecheck.utils.emptyDataSource
import modulecheck.utils.lazySet
import modulecheck.utils.requireNotNull
import java.io.File
import java.nio.charset.Charset

abstract class ProjectTest : BaseTest() {

  val projectCache: ProjectCache by resets { ProjectCache() }

  val projectProvider: ProjectProvider by resets {
    object : ProjectProvider {

      override val projectCache: ProjectCache
        get() = this@ProjectTest.projectCache

      override fun get(path: String): McProject {
        return projectCache.getValue(path)
      }

      override fun getAll(): List<McProject> = allProjects()

      override fun clearCaches() {
        allProjects().forEach { it.clearContext() }
      }
    }
  }

  fun project(path: String, config: McProjectBuilderScope.() -> Unit = {}): McProject {

    return createProject(projectCache, testProjectDir, path, config)
  }

  fun McProject.toBuilder(): McProjectBuilderScope {

    return JvmMcProjectBuilderScope(
      path = path,
      projectDir = projectDir,
      buildFile = buildFile,
      configurations = configurations.toMutableMap(),
      projectDependencies = projectDependencies,
      externalDependencies = externalDependencies,
      hasKapt = hasKapt,
      sourceSets = sourceSets.toMutableMap(),
      anvilGradlePlugin = anvilGradlePlugin,
      projectCache = projectCache
    )
  }

  fun AndroidMcProject.toBuilder(): AndroidMcProjectBuilderScope = runBlocking {

    androidBasePackagesForSourceSetName(SourceSetName.MAIN)
      .requireNotNull {
        "The receiver Android project's base package property can't be null here."
      }

    RealAndroidMcProjectBuilderScope(
      path = path,
      projectDir = projectDir,
      buildFile = buildFile,
      androidResourcesEnabled = androidResourcesEnabled,
      viewBindingEnabled = viewBindingEnabled,
      kotlinAndroidExtensionEnabled = kotlinAndroidExtensionEnabled,
      manifests = manifests.toMutableMap(),
      configurations = configurations.toMutableMap(),
      projectDependencies = projectDependencies,
      externalDependencies = externalDependencies,
      hasKapt = hasKapt,
      sourceSets = sourceSets.toMutableMap(),
      anvilGradlePlugin = anvilGradlePlugin,
      projectCache = projectCache
    )
  }

  fun McProject.edit(config: McProjectBuilderScope.() -> Unit = {}): McProject {
    return toBuilder()
      .also { it.config() }
      .toProject()
  }

  fun AndroidMcProject.edit(config: AndroidMcProjectBuilderScope.() -> Unit = {}): McProject {
    return toBuilder()
      .also { it.config() }
      .toProject()
  }

  fun McProjectBuilderScope.childProject(
    path: String,
    config: McProjectBuilderScope.() -> Unit = {}
  ): McProject {

    val appendedPath = (this@childProject.path + path).replace(":{2,}".toRegex(), ":")

    return createProject(projectCache, testProjectDir, appendedPath, config)
  }

  fun androidProject(
    path: String,
    androidPackage: String,
    config: AndroidMcProjectBuilderScope.() -> Unit = {}
  ): McProject {

    return createAndroidProject(
      projectCache = projectCache,
      projectDir = testProjectDir,
      path = path,
      androidPackage = androidPackage,
      config = config
    )
  }

  fun McProjectBuilderScope.androidChildProject(
    path: String,
    androidPackage: String,
    config: AndroidMcProjectBuilderScope.() -> Unit = {}
  ): McProject {

    val appendedPath = (this@androidChildProject.path + path).replace(":{2,}".toRegex(), ":")

    return createAndroidProject(
      projectCache = projectCache,
      projectDir = testProjectDir,
      path = appendedPath,
      androidPackage = androidPackage,
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
  ) = project(path) {

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
          .let { lazySet(it, emptyDataSource()) }
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
                |Project ${project.path} has a reference which must be declared in a dependency project.
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
