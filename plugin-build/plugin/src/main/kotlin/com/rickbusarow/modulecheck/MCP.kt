package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.internal.*
import org.gradle.api.Project
import java.util.concurrent.ConcurrentHashMap

class MCP private constructor(
  val project: Project
) : Comparable<MCP> {

  init {
    cache[project] = this
  }

  val path: String = project.path

  val dependencies = DependencyParser.parse(this)

  val overshot by lazy { OvershotParser.parse(this) }
  val unused by lazy { UnusedParser.parse(this) }
//  val redundant = Parsed<DependencyFinding.RedundantDependency>()

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

  val androidTestDeclarations = androidTestFiles.flatMap { it.declarations }.toSet()
  val mainDeclarations = mainFiles.flatMap { it.declarations }.toSet()
  val testDeclarations = testFiles.flatMap { it.declarations }.toSet()

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


  override fun compareTo(other: MCP): Int = project.path.compareTo(other.project.path)

  fun positionIn(parent: Project, configuration: String): Position =
    parent
      .buildFile
      .readText()
      .lines()
      .positionOf(project.project, configuration)

  data class Position(val row: Int, val column: Int)

  class Parsed<T>(
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

