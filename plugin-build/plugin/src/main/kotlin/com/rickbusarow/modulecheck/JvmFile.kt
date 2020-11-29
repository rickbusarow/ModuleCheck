package com.rickbusarow.modulecheck

import org.gradle.api.Project
import org.jetbrains.kotlin.psi.KtImportDirective
import java.util.concurrent.ConcurrentHashMap

data class JvmFile(val packageFqName: String, val importDirectives: Set<KtImportDirective>)
data class XmlFile(val customViews: Set<String>, val resourceReferences: Set<String>)

sealed class ModuleCheckProject : Comparable<ModuleCheckProject> {
  abstract val path: String
  abstract val project: Project
  abstract val mainPackages: Set<String>
  abstract val mainImports: Set<String>
  abstract val mainDependencies: Set<Project>
  abstract val testPackages: Set<String>
  abstract val testImports: Set<String>
  abstract val testDependencies: Set<Project>

  fun init() {
    @Suppress("LeakingThis")
    cache[project] = this
  }

  val depth: Int by lazy {
    if (mainDependencies.isEmpty()) 0 else (mainDependencies.map { cache[it]!!.depth }.max()!! + 1)
  }

//  val importCandidates by lazy { importCandidates() }

  override fun compareTo(other: ModuleCheckProject): Int = path.compareTo(other.path)

  override fun toString(): String {
    return """ModuleCheckProject( path='$path' )"""
  }

  data class JavaModuleCheckProject(
    override val path: String,
    override val project: Project,
    override val mainPackages: Set<String>,
    override val mainImports: Set<String>,
    override val mainDependencies: Set<Project>,
    override val testPackages: Set<String>,
    override val testImports: Set<String>,
    override val testDependencies: Set<Project>
  ) : ModuleCheckProject()

  data class AndroidModuleCheckProject(
    override val path: String,
    override val project: Project,
    override val mainPackages: Set<String>,
    override val mainImports: Set<String>,
    override val mainDependencies: Set<Project>,
    override val testPackages: Set<String>,
    override val testImports: Set<String>,
    override val testDependencies: Set<Project>,
    val androidTestPackages: Set<String>,
    val androidTestImports: Set<String>,
    val androidTestDependencies: Set<Project>
  ) : ModuleCheckProject()


  companion object {
    private val cache = ConcurrentHashMap<Project, ModuleCheckProject>()
  }
}
