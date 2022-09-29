/*
 * Copyright (C) 2021-2022 Rick Busarow
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

package modulecheck.finding

import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.ProjectDependency
import modulecheck.project.McProject

data class RedundantDependency(
  val dependentProject: McProject,
  val dependency: ProjectDependency,
  val configurationName: ConfigurationName,
  val from: List<ProjectDependency>
) {
  fun toFinding(findingName: FindingName): RedundantDependencyFinding = RedundantDependencyFinding(
    findingName = findingName,
    dependentProject = dependentProject,
    oldDependency = dependency,
    configurationName = configurationName,
    from = from
  )
}

data class RedundantDependencyFinding(
  override val findingName: FindingName,
  override val dependentProject: McProject,
  override val oldDependency: ProjectDependency,
  override val configurationName: ConfigurationName,
  val from: List<ProjectDependency>
) : AbstractProjectDependencyFinding(),
  RemovesDependency,
  Deletable {

  override val dependency get() = oldDependency

  override val message: String
    get() = "The dependency is declared as `api` in a dependency module, but also explicitly " +
      "declared in the current module.  This is technically unnecessary if a \"minimalist\" build " +
      "file is desired."

  override val dependencyIdentifier = oldDependency.projectPath.value + fromStringOrEmpty()

  override fun fromStringOrEmpty(): String {

    return if (from.all { dependency.projectPath == it.projectPath }) {
      ""
    } else {
      from.joinToString { it.projectPath.value }
    }
  }
}
