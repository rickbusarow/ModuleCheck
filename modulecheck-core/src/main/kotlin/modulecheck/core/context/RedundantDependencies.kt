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

import modulecheck.api.Deletable
import modulecheck.api.context.publicDependencies
import modulecheck.core.DependencyFinding
import modulecheck.parsing.ConfigurationName
import modulecheck.parsing.McProject
import modulecheck.parsing.ProjectContext
import modulecheck.parsing.asConfigurationName
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class RedundantDependencyFinding(
  override val dependentPath: String,
  override val buildFile: File,
  override val dependencyProject: McProject,
  val dependencyPath: String,
  override val configurationName: ConfigurationName,
  val from: List<McProject>
) : DependencyFinding("redundant"),
  Deletable {

  override val message: String
    get() = "The dependency is declared as `api` in a dependency module, but also explicitly " +
      "declared in the current module.  This is technically unnecessary if a \"minimalist\" build " +
      "file is desired."

  override val dependencyIdentifier = dependencyPath + fromStringOrEmpty()

  override fun fromStringOrEmpty(): String {

    return if (from.all { dependencyProject.path == it.path }) {
      ""
    } else {
      from.joinToString { it.path }
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
    override operator fun invoke(project: McProject): RedundantDependencies {
      val allApi = project
        .projectDependencies["api".asConfigurationName()]
        .orEmpty()
        .toSet()

      val inheritedDependencyProjects = project
        .projectDependencies
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
