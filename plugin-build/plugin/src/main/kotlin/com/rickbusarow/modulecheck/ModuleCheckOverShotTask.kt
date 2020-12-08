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
      .map { gradleProject -> ModuleCheckProject.from(gradleProject) }

}

class OverShotProvider(
  project: Project, alwaysIgnore: Set<String>, ignoreAll: Set<String>
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
          listOf(
            findings.overshotApiDependencies(),
            findings.overshotImplementationDependencies()
          )
            .flatMap { dependencies ->
              dependencies.mapNotNull { dependency ->
                if (dependency.dependencyPath in alwaysIgnore) {
                  null
                } else {
                  dependency
                }
              }
            }
            .distinctBy { it.dependencyPath }
        }
      }
  }
}

class RedundantProvider(
  project: Project, alwaysIgnore: Set<String>, ignoreAll: Set<String>
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
          listOf(
            findings.redundantAndroidTest(),
            findings.redundantApi(),
            findings.redundantCompileOnly(),
            findings.redundantImplementation(),
            findings.redundantTest()
          ).flatMap { dependencies ->
            dependencies.mapNotNull { dependency ->
              if (dependency.dependencyPath in alwaysIgnore) {
                null
              } else {
                dependency
              }
            }
          }
            .distinctBy { it.position() }
        }
      }
  }
}

class UnusedProvider(
  project: Project, alwaysIgnore: Set<String>, ignoreAll: Set<String>
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
          listOf(
            findings.unusedAndroidTest(),
            findings.unusedApi(),
            findings.unusedCompileOnly(),
            findings.unusedImplementation(),
            findings.unusedTestImplementation()
          ).flatMap { dependencies ->
            dependencies.mapNotNull { dependency ->
              if (dependency.dependencyPath in alwaysIgnore) {
                null
              } else {
                dependency
              }
            }
          }
            .distinctBy { it.position() }
        }
      }
  }
}

