package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.internal.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

class ModuleCheckPlugin : Plugin<Project> {

  override fun apply(project: Project) {

    project.tasks.register("moduleCheck", ModuleCheckTask::class.java)
  }
}


fun Project.toModuleCheckProject(): ModuleCheckProject.JavaModuleCheckProject {

//  println("build file --> ${buildFile}")

  val mainFiles = mainJavaRoot.walkTopDown()
    .files()
    .filter { it.name.endsWith(".kt") }
    .ktFiles()
    .map { JvmFile(it.packageFqName.asString(), it.importDirectives.toSet()) }
    .toList()

  val testFiles = testJavaRoot.walkTopDown()
    .files()
    .filter { it.name.endsWith(".kt") }
    .ktFiles()
    .map { JvmFile(it.packageFqName.asString(), it.importDirectives.toSet()) }
    .toList()

  val mainPackages = mainFiles.map { it.packageFqName }.toSet()

  val xmlParser = AndroidLayoutParser()

  val mainLayoutDependencies = mainLayoutRootOrNull()?.walkTopDown()
    ?.files()
    ?.map { xmlParser.parse(it) }
    ?.flatten()
    ?.toSet().orEmpty()

  val mainImports = mainFiles.flatMap {
    it.importDirectives.mapNotNull { importDirective ->

      importDirective.importPath
        ?.pathStr
        ?.split(".")
        ?.dropLast(1)
        ?.joinToString(".")
    } + mainLayoutDependencies.map { layoutDependency ->
      layoutDependency.split(".")
        .dropLast(1)
        .joinToString(".")
    }
  }.toSet()

  val testPackages = testFiles.map { it.packageFqName }.toSet()
  val testImports = testFiles.flatMap {
    it.importDirectives.mapNotNull { importDirective ->

      importDirective.importPath
        ?.pathStr
        ?.split(".")
        ?.dropLast(1)
        ?.joinToString(".")
    }
  }.toSet()

  val mainDependencyProjects = configurations
    .filter { it.name == "api" || it.name == "implementation" }
    .flatMap { config ->

      config.dependencies
        .withType(ProjectDependency::class.java)
        .map { it.dependencyProject }
    }.toSet()

  val testDependencyProjects = configurations
    .filter { it.name == "testApi" || it.name == "testImplementation" }
    .flatMap { config ->

      config.dependencies
        .withType(ProjectDependency::class.java)
        .map { it.dependencyProject }
    }.toSet()

  return ModuleCheckProject.JavaModuleCheckProject(
    path = path,
    project = this,
    mainPackages = mainPackages,
    mainImports = mainImports,
    mainDependencies = mainDependencyProjects,
    testPackages = testPackages,
    testImports = testImports,
    testDependencies = testDependencyProjects
  )
}
