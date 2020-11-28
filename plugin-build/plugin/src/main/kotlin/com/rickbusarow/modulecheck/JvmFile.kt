package com.rickbusarow.modulecheck

import org.gradle.api.Project
import org.jetbrains.kotlin.psi.KtImportDirective

data class JvmFile(val packageFqName: String, val importDirectives: Set<KtImportDirective>)
data class XmlFile(val customViews: Set<String>, val resourceReferences: Set<String>)

class IntermediateModuleCheckProject(
  val path: String,
  val project: Project,
  val mainPackages: Set<String>,
  val mainImports: Set<String>,
  val mainDependencies: Set<Project>,
  val testPackages: Set<String>,
  val testImports: Set<String>,
  val testDependencies: Set<Project>,
  importCandidates: () -> Set<String>
) : Comparable<ModuleCheckProject> {

  val importCandidates by lazy { importCandidates() }

  override fun compareTo(other: ModuleCheckProject): Int = path.compareTo(other.path)

  override fun toString(): String {
    return """ModuleCheckProject(
          path='$path',
    )"""
  }

}

class ModuleCheckProject(
  val path: String,
  val project: Project,
  val mainPackages: Set<String>,
  val mainImports: Set<String>,
  val mainDependencies: Map<Project, ModuleCheckProject>,
  val testPackages: Set<String>,
  val testImports: Set<String>,
  val testDependencies: Map<Project, ModuleCheckProject>,
  importCandidates: () -> Set<String>
) : Comparable<ModuleCheckProject> {

  val depth: Int by lazy {
    if (mainDependencies.isEmpty()) 0 else (mainDependencies.values.map { it.depth }.max()!! + 1)
  }

  val importCandidates by lazy { importCandidates() }

  override fun compareTo(other: ModuleCheckProject): Int = path.compareTo(other.path)

  override fun toString(): String {
    return """ModuleCheckProject(
          path='$path',
    )"""
  }

}
