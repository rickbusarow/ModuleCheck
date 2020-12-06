package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.internal.*
import org.gradle.api.Project
import java.util.concurrent.ConcurrentHashMap

class ModuleCheckProject private constructor(
  val project: Project
) : Comparable<ModuleCheckProject> {

  init {
    cache[project] = this
  }

  val path: String = project.path

  val dependencies = ProjectDependencies(this)
  val findings = ProjectFindings(this)

  val androidTestFiles by lazy {
    project.androidTestJavaRoot.jvmFiles() + project.androidTestKotlinRoot.jvmFiles()
  }
  val mainFiles by lazy {
    project.mainJavaRoot.jvmFiles() + project.mainKotlinRoot.jvmFiles()
  }
  val testFiles by lazy {
    project.testJavaRoot.jvmFiles() + project.testKotlinRoot.jvmFiles()
  }
  val androidTestImports by lazy {
    androidTestFiles.flatMap { jvmFile -> jvmFile.importDirectives }.toSet()
  }

  val mainImports by lazy {
    (mainFiles.flatMap { jvmFile -> jvmFile.importDirectives } + dependencies.mainLayoutViewDependencies).toSet()
  }

  val testImports by lazy {
    testFiles.flatMap { jvmFile -> jvmFile.importDirectives }.toSet()
  }

  val androidTestPackages by lazy { androidTestFiles.map { it.packageFqName }.toSet() }
  val mainPackages by lazy { mainFiles.map { it.packageFqName }.toSet() }
  val testPackages by lazy { testFiles.map { it.packageFqName }.toSet() }

  val mainLayoutFiles by lazy {
    project.mainLayoutRootOrNull()?.walkTopDown()?.files().orEmpty().map { XmlFile.LayoutFile(it) }
  }

  fun allPublicClassPathDependencyDeclarations(): List<ModuleCheckProject> =
    dependencies.apiDependencies + dependencies.apiDependencies.flatMap {
      it.allPublicClassPathDependencyDeclarations()
    }

  fun inheritedMainDependencyProjects(): List<ModuleCheckProject> {

    val main = dependencies.apiDependencies + dependencies.implementationDependencies

    return dependencies.apiDependencies + main.flatMap { pdd ->

      pdd.inheritedMainDependencyProjects() +
        pdd.dependencies.apiDependencies.flatMap {
          it.inheritedMainDependencyProjects()
        }
    }
  }

  fun positionIn(parent: Project): ModuleCheckProject.Position =
    parent.buildFile.readText().lines().positionOf(project.project)


  private fun DependencyFinding.moduleCheckProject() = cache.getValue(dependentProject)

  override fun compareTo(other: ModuleCheckProject): Int = path.compareTo(other.path)

  override fun toString(): String {
    return """ModuleCheckProject( path='$path' )"""
  }

  data class Position(val row: Int, val column: Int)

  companion object {
    private val cache = ConcurrentHashMap<Project, ModuleCheckProject>()

    fun reset() {
      cache.clear()
    }

    fun from(project: Project) = cache.getOrPut(project) { ModuleCheckProject(project) }

    fun dependentsOf(project: Project): List<ModuleCheckProject> {

      val dep = from(project)

      return cache.values.filter {
        it.dependencies.implementationDependencies.any { it == dep } ||
          it.dependencies.mainDependencies.any { it == dep } ||
          it.dependencies.testImplementationDependencies.any { it == dep } ||
          it.dependencies.androidTestImplementationDependencies.any { it == dep } ||
          it.dependencies.compileOnlyDependencies.any { it == dep }
      }
    }
  }
}
