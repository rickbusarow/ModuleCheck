/*
 * Copyright (C) 2020 Rick Busarow
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rickbusarow.modulecheck

import org.gradle.api.Project

abstract class FindingProvider<T : DependencyFinding>(
  protected val project: Project,
  protected val alwaysIgnore: Set<String>,
  protected val ignoreAll: Set<String>
) {

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
