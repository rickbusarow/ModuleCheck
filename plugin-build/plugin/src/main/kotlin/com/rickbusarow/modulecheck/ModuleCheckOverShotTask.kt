package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.internal.Cli
import org.gradle.api.Project

abstract class FindingProvider<T : DependencyFinding>(
  protected val project: Project,
  protected val alwaysIgnore: Set<String>,
  protected val ignoreAll: Set<String>
) {

  private val cli = Cli()

  abstract fun get(): List<T>

  protected fun Project.moduleCheckProjects() =
    project.rootProject.allprojects
      .filter { gradleProject -> gradleProject.buildFile.exists() }
      .map { gradleProject -> MCP.from(gradleProject) }
}

class OverShotProvider(
  project: Project,
  alwaysIgnore: Set<String>,
  ignoreAll: Set<String>
) : FindingProvider<DependencyFinding.OverShotDependency>(
  project, alwaysIgnore, ignoreAll
) {

  override fun get(): List<DependencyFinding.OverShotDependency> {
    return project
      .moduleCheckProjects()
      .sorted()
      .filterNot { moduleCheckProject -> moduleCheckProject.path in ignoreAll }
      .flatMap { moduleCheckProject ->
        with(moduleCheckProject) {
          overshot
            .all()
            .mapNotNull { dependency ->
              if (dependency.dependencyPath in alwaysIgnore) {
                null
              } else {
                dependency
              }
            }
            .distinctBy { it.dependencyPath }
        }
      }
  }
}

class RedundantProvider(
  project: Project,
  alwaysIgnore: Set<String>,
  ignoreAll: Set<String>
) : FindingProvider<DependencyFinding.RedundantDependency>(
  project, alwaysIgnore, ignoreAll
) {

  override fun get(): List<DependencyFinding.RedundantDependency> {
    return project
      .moduleCheckProjects()
      .sorted()
      .filterNot { moduleCheckProject ->
        moduleCheckProject.path in ignoreAll
      }
      .flatMap { moduleCheckProject ->
        with(moduleCheckProject) {
          redundant
            .all()
            .mapNotNull { dependency ->
              if (dependency.dependencyPath in alwaysIgnore) {
                null
              } else {
                dependency
              }
            }
            .distinctBy { it.position() }
        }
      }
  }
}

class UnusedProvider(
  project: Project,
  alwaysIgnore: Set<String>,
  ignoreAll: Set<String>
) : FindingProvider<DependencyFinding.UnusedDependency>(
  project, alwaysIgnore, ignoreAll
) {

  override fun get(): List<DependencyFinding.UnusedDependency> {
    return project
      .moduleCheckProjects()
      .sorted()
      .filterNot { moduleCheckProject ->
        moduleCheckProject.path in ignoreAll
      }
      .flatMap { moduleCheckProject ->
        with(moduleCheckProject) {
          unused
            .all()
            .mapNotNull { dependency ->
              if (dependency.dependencyPath in alwaysIgnore) {
                null
              } else {
                dependency
              }
            }
            .distinctBy { it.position() }
        }
      }
  }
}
