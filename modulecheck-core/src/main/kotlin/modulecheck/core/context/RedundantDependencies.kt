/*
 * Copyright (C) 2021 Rick Busarow
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

package modulecheck.core.context

import modulecheck.api.*
import modulecheck.api.context.ProjectContext
import modulecheck.core.DependencyFinding
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class RedundantDependencyFinding(
  override val dependentPath: String,
  override val buildFile: File,
  override val dependencyProject: Project2,
  val dependencyPath: String,
  override val configurationName: ConfigurationName,
  val from: List<Project2>
) : DependencyFinding("redundant"),
    Deletable {

  override val dependencyIdentifier = dependencyPath + fromStringOrEmpty()

  private fun fromStringOrEmpty(): String {

    return if (from.all { dependencyProject.path == it.path }) {
      ""
    } else {
      " from: ${from.joinToString { it.path }}"
    }
  }
}

data class RedundantDependencies(
  internal val delegate: ConcurrentMap<ConfigurationName, Set<RedundantDependencyFinding>>
) : ConcurrentMap<ConfigurationName, Set<RedundantDependencyFinding>> by delegate,
    ProjectContext.Element {

  override val key: ProjectContext.Key<RedundantDependencies>
    get() = Key

  companion object Key : ProjectContext.Key<RedundantDependencies> {
    override operator fun invoke(project: Project2): RedundantDependencies {
      val allApi = project
        .projectDependencies
        .value["api".asConfigurationName()]
        .orEmpty()
        .toSet()

      val inheritedDependencyProjects = project
        .projectDependencies
        .value
        .main()
        .flatMap {
          it
            .project
            .publicDependencies
            .map { it.project }
            .toSet()
        }

      val redundant = allApi
        .filter { it.project in inheritedDependencyProjects }
        .map {
          val from = allApi
            .filter { inherited -> inherited.project == it.project }
            .map { it.project }

          RedundantDependencyFinding(
            dependentPath = project.path,
            buildFile = project.buildFile,
            dependencyProject = it.project,
            dependencyPath = it.project.path,
            configurationName = it.configurationName,
            from = from
          )
        }

      val grouped = redundant
        .groupBy { it.configurationName }
        .mapValues { it.value.toSet() }

      return RedundantDependencies(ConcurrentHashMap(grouped))
    }
  }
}

val ProjectContext.redundantDependencies: RedundantDependencies get() = get(RedundantDependencies)
