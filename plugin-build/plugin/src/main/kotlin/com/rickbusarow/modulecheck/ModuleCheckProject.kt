package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.internal.*
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KProperty1


data class ModuleCheckProject(val project: Project) : Comparable<ModuleCheckProject> {

  init {
    cache[project] = this
  }

  val path: String = project.path

  val androidTestFiles by unsafeLazy { project.androidTestJavaRoot.jvmFiles() + project.androidTestKotlinRoot.jvmFiles() }
  val mainFiles by unsafeLazy { project.mainJavaRoot.jvmFiles() + project.mainKotlinRoot.jvmFiles() }
  val testFiles by unsafeLazy { project.testJavaRoot.jvmFiles() + project.testKotlinRoot.jvmFiles() }

  val androidTestPackages by unsafeLazy { androidTestFiles.map { it.packageFqName }.toSet() }
  val mainPackages by unsafeLazy { mainFiles.map { it.packageFqName }.toSet() }
  val testPackages by unsafeLazy { testFiles.map { it.packageFqName }.toSet() }

  val mainLayoutFiles by unsafeLazy { project.mainLayoutRootOrNull()?.walkTopDown()?.files().orEmpty() }

  val mainLayoutViewDependencies by unsafeLazy {
    mainLayoutFiles
      .map { AndroidLayoutParser.parse(it) }
      .flatten()
      .map {
        it.split(".")
          .dropLast(1)
          .joinToString(".")
      }
      .toSet()
  }

  val androidTestImports by unsafeLazy {
    androidTestFiles.flatMap { jvmFile ->
      jvmFile.importDirectives
    }.toSet()
  }

  val mainImports by unsafeLazy {
    (mainFiles.flatMap { jvmFile ->
      jvmFile.importDirectives
    } + mainLayoutViewDependencies).toSet()
  }

  val testImports by unsafeLazy {
    testFiles.flatMap { jvmFile ->
      jvmFile.importDirectives
    }.toSet()
  }

  val compileOnlyDependencies by unsafeLazy { dependencyProjects("compileOnly") }
  val apiDependencies by unsafeLazy { dependencyProjects("api") }
  val implementationDependencies by unsafeLazy { dependencyProjects("implementation") }
  val testImplementationDependencies by unsafeLazy { dependencyProjects("testImplementation") }
  val androidTestImplementationDependencies by unsafeLazy { dependencyProjects("androidTestImplementation") }

  val mainDepth: Int by unsafeLazy {

    val all = compileOnlyDependencies + apiDependencies + implementationDependencies

    if (all.isEmpty()) 0 else (all.map { cache.getValue(it.project).mainDepth }.max()!! + 1)
  }

  val testDepth: Int by unsafeLazy {

    if (testImplementationDependencies.isEmpty()) 0 else (testImplementationDependencies.map { cache.getValue(it.project).mainDepth }
      .max()!! + 1)
  }

  val androidTestDepth: Int by unsafeLazy {

    if (androidTestImplementationDependencies.isEmpty()) 0 else (androidTestImplementationDependencies.map {
      cache.getValue(it.project).mainDepth
    }.max()!! + 1)
  }

  val unusedAndroidTest by unsafeLazy {
    findUnused(androidTestImports, ModuleCheckProject::androidTestImplementationDependencies, "androidTest")
  }
  val unusedApi by unsafeLazy {
    findUnused(mainImports, ModuleCheckProject::apiDependencies, "api")
  }
  val unusedCompileOnly by unsafeLazy {
    findUnused(mainImports, ModuleCheckProject::compileOnlyDependencies, "compileOnly")
  }
  val unusedImplementation by unsafeLazy {
    findUnused(mainImports, ModuleCheckProject::implementationDependencies, "implementation")
  }
  val unusedTestImplementation by unsafeLazy {
    findUnused(testImports, ModuleCheckProject::testImplementationDependencies, "testImplementation")
  }

  private fun findUnused(
    imports: Set<String>,
    dependencyKProp: KProperty1<ModuleCheckProject, List<ProjectDependencyDeclaration>>,
    configurationName: String
  ): List<UnusedDependency> = dependencyKProp(this)
    // .filterNot { alwaysIgnore.contains(it.project.path)}
    .mapNotNull { projectDependency ->

      val dependencyFromCache = cache[projectDependency.project]

      require(dependencyFromCache != null) { "cache does not contain $projectDependency \n\n${cache.keys}" }

      val used = imports.any { importString ->
        when {
          dependencyFromCache.mainPackages.contains(importString) -> true
          else -> dependencyKProp(this).any { childProjectDependency ->

            val childModuleCheckProject = cache.getValue(childProjectDependency.project)

            dependencyKProp(childModuleCheckProject).contains(projectDependency)
          }
        }
      }

      if (!used) {
        UnusedDependency(project, projectDependency.position, projectDependency.project.path, configurationName)
      } else null
    }

  private fun dependencyProjects(configurationName: String) = project.configurations
    .filter { it.name == configurationName }
    .flatMap { config ->
      config.dependencies
        .withType(ProjectDependency::class.java)
        .map { ProjectDependencyDeclaration(project = it.dependencyProject, dependent = project) }
        .toSet()
    }

  override fun compareTo(other: ModuleCheckProject): Int = path.compareTo(other.path)

  override fun toString(): String {
    return """ModuleCheckProject( path='$path' )"""
  }


  companion object {
    private val cache = ConcurrentHashMap<Project, ModuleCheckProject>()
  }
}
