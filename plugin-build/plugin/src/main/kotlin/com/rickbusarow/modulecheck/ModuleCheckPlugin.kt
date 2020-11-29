package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.internal.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.setProperty

abstract class ModuleCheckExtension(objects: ObjectFactory) {

  val alwaysIgnore: SetProperty<String> = objects.setProperty<String>()
}

fun Project.moduleCheck(config: ModuleCheckExtension.() -> Unit) {
  extensions.configure(ModuleCheckExtension::class, config)
}

class ModuleCheckPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    target.extensions.create("moduleCheck", ModuleCheckExtension::class.java)
    target.tasks.register("moduleCheck", ModuleCheckTask::class.java)
  }
}

internal fun List<String>.positionOf(project: Project): ProjectDependencyDeclaration.Position {

  val reg = """.*project[(]{0,1}(?:path =\s*)"${project.path}".*""".toRegex()

  val row = indexOfFirst { it.trim().matches(reg) }

  val col = if (row == -1) -1 else get(row).indexOfFirst { it != ' ' }

  return ProjectDependencyDeclaration.Position(row + 1, col + 1)
}

fun Project.toModuleCheckProject(): ModuleCheckProject.JavaModuleCheckProject {

  val buildFileLines = if (buildFile.exists()) buildFile.readText().lines() else emptyList()

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
        .map { ProjectDependencyDeclaration(it.dependencyProject, buildFileLines.positionOf(it.dependencyProject)) }
    }.toSet()

  val testDependencyProjects = configurations
    .filter { it.name == "testApi" || it.name == "testImplementation" }
    .flatMap { config ->

      config.dependencies
        .withType(ProjectDependency::class.java)
        .map { ProjectDependencyDeclaration(it.dependencyProject, buildFileLines.positionOf(it.dependencyProject)) }
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
