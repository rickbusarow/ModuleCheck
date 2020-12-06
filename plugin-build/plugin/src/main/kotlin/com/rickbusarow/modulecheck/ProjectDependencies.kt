package com.rickbusarow.modulecheck

import org.gradle.api.artifacts.ProjectDependency

class ProjectDependencies(private val project: ModuleCheckProject) {

  val compileOnlyDependencies by lazy { dependencyProjects("compileOnly") }
  val apiDependencies by lazy { dependencyProjects("api") }
  val implementationDependencies by lazy { dependencyProjects("implementation") }
  val mainDependencies by lazy {
    compileOnlyDependencies + apiDependencies + implementationDependencies
  }
  val testImplementationDependencies by lazy { dependencyProjects("testImplementation") }
  val androidTestImplementationDependencies by lazy {
    dependencyProjects("androidTestImplementation")
  }

  val mainLayoutViewDependencies by lazy {
    project.mainLayoutFiles.map { it.customViews }.flatten().toSet()
  }

  val mainLayoutResourceDependencies by lazy {
    project.mainLayoutFiles.map { it.customViews }.flatten().toSet()
  }

  fun sourceOf(
    dependencyProject: ModuleCheckProject,
    apiOnly: Boolean = false
  ): ModuleCheckProject? {

    val toCheck = if (apiOnly) apiDependencies else mainDependencies

    if (dependencyProject in toCheck) return this.project

    return toCheck.firstOrNull {
      it == dependencyProject || it.dependencies.sourceOf(dependencyProject, true) != null
    }
  }

  private fun dependencyProjects(configurationName: String) =
    project.project
      .configurations
      .filter { it.name == configurationName }
      .flatMap { config ->
        config
          .dependencies
          .withType(ProjectDependency::class.java)
          .map { ModuleCheckProject.from(it.dependencyProject) }
          .toSet()
      }
}
