/*
 * Copyright (C) 2020 Rick Busarow
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

package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.internal.*
import com.rickbusarow.modulecheck.parser.*
import org.gradle.api.Project
import java.util.concurrent.ConcurrentHashMap

class MCP private constructor(
  val project: Project
) : Comparable<MCP> {

  init {
    cache[project] = this
  }

  val path: String = project.path

  val dependencies by DependencyParser.parseLazy(this)
  val kaptDependencies by KaptParser.parseLazy(this)

  val resolvedMainDependencies by lazy {

    val all = dependencies.main() + allPublicClassPathDependencyDeclarations()

    all.filter { dep ->
      dep.mcp().mainDeclarations.any { it in mainImports }
    }
  }

  val overshot by OvershotParser.parseLazy(this)
  val unused by UnusedParser.parseLazy(this)
  val unusedKapt by UnusedKaptParser.parseLazy(this)
  val redundant by RedundantParser.parseLazy(this)

  val androidTestFiles =
    project.androidTestJavaRoot.jvmFiles() + project.androidTestKotlinRoot.jvmFiles()
  val mainFiles = project.mainJavaRoot.jvmFiles() + project.mainKotlinRoot.jvmFiles()
  val testFiles = project.testJavaRoot.jvmFiles() + project.testKotlinRoot.jvmFiles()

  val mainLayoutFiles = project
    .mainLayoutRootOrNull()
    ?.walkTopDown()
    ?.files()
    .orEmpty()
    .map { XmlFile.LayoutFile(it) }

  val androidTestImports = androidTestFiles.flatMap { jvmFile -> jvmFile.importDirectives }.toSet()

  val mainImports = (
    mainFiles
      .flatMap { jvmFile -> jvmFile.importDirectives } + mainLayoutFiles.map { it.customViews }
      .flatten()
      .toSet()
    ).toSet()

  val testImports = testFiles.flatMap { jvmFile -> jvmFile.importDirectives }.toSet()

  val androidTestDeclarations by lazy { androidTestFiles.flatMap { it.declarations }.toSet() }
  val mainDeclarations by lazy { mainFiles.flatMap { it.declarations }.toSet() }
  val testDeclarations by lazy { testFiles.flatMap { it.declarations }.toSet() }

  fun dependents() =
    cache.values.filter { it.dependencies.all().any { it.project == this.project } }

  fun allPublicClassPathDependencyDeclarations(): Set<CPP> =
    dependencies.api + dependencies.api.flatMap {
      it.mcp().allPublicClassPathDependencyDeclarations()
    }

  fun inheritedMainDependencyProjects(): List<MCP> {
    return dependencies.main().flatMap { pdd ->

      pdd.mcp().inheritedMainDependencyProjects() + pdd.mcp().dependencies.api.flatMap {
        it.mcp().inheritedMainDependencyProjects()
      }
    }
  }

  fun sourceOf(
    dependencyProject: CPP,
    apiOnly: Boolean = false
  ): MCP? {
    val toCheck = if (apiOnly) dependencies.api else dependencies.main()

    if (dependencyProject in toCheck) return this

    return toCheck.firstOrNull {
      it == dependencyProject || it.mcp().sourceOf(dependencyProject, true) != null
    }
      ?.mcp()
  }

  fun getMainDepth(): Int {
    val all = dependencies.main()

    return if (all.isEmpty()) 0
    else all.map { it.mcp().getMainDepth() }.max()!! + 1
  }

  fun getTestDepth(): Int = if (dependencies.testImplementation.isEmpty()) {
    0
  } else {
    dependencies.testImplementation
      .map { it.mcp().getMainDepth() }
      .max()!! + 1
  }

  val androidTestDepth: Int
    get() = if (dependencies.androidTest.isEmpty()) {
      0
    } else {
      dependencies.androidTest
        .map { it.mcp().getMainDepth() }
        .max()!! + 1
    }

  override fun compareTo(other: MCP): Int = project.path.compareTo(other.project.path)

  fun positionIn(parent: Project, configuration: String): Position =
    parent
      .buildFile
      .readText()
      .lines()
      .positionOf(project.project, configuration)

  data class Position(val row: Int, val column: Int)

  data class ParsedKapt<T>(
    val androidTest: Set<T>,
    val main: Set<T>,
    val test: Set<T>
  ) {
    fun all() = androidTest + main + test
  }

  data class Parsed<T>(
    val androidTest: MutableSet<T>,
    val api: MutableSet<T>,
    val compileOnly: MutableSet<T>,
    val implementation: MutableSet<T>,
    val runtimeOnly: MutableSet<T>,
    val testApi: MutableSet<T>,
    val testImplementation: MutableSet<T>
  ) {
    fun all() =
      androidTest + api + compileOnly + implementation + runtimeOnly + testApi + testImplementation

    fun main() = api + compileOnly + implementation + runtimeOnly
  }

  companion object {
    private val cache = ConcurrentHashMap<Project, MCP>()

    fun reset() {
      Output.printGreen("                                                          resetting")
      cache.clear()
    }

    fun from(project: Project): MCP = cache.getOrPut(project) { MCP(project) }
  }
}

@JvmName("CppCollectionToMCP")
fun Collection<CPP>.mcp() = map { MCP.from(it.project) }
fun CPP.mcp() = MCP.from(this.project)

@JvmName("ProjectCollectionToMCP")
fun Collection<Project>.mcp() = map { MCP.from(it.project) }
fun Project.mcp() = MCP.from(this)
