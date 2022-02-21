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

package modulecheck.core

import modulecheck.api.finding.Deletable
import modulecheck.api.finding.RemovesDependency
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject

data class RedundantDependencyFinding(
  override val dependentProject: McProject,
  override val oldDependency: ConfiguredProjectDependency,
  override val configurationName: ConfigurationName,
  val from: List<ConfiguredProjectDependency>
) : AbstractProjectDependencyFinding("redundant"),
  RemovesDependency,
  Deletable {

  override val dependencyProject = oldDependency.project

  override val message: String
    get() = "The dependency is declared as `api` in a dependency module, but also explicitly " +
      "declared in the current module.  This is technically unnecessary if a \"minimalist\" build " +
      "file is desired."

  override val dependencyIdentifier = oldDependency.project.path + fromStringOrEmpty()

  override fun fromStringOrEmpty(): String {

    return if (from.all { dependencyProject.path == it.project.path }) {
      ""
    } else {
      from.joinToString { it.project.path }
    }
  }
}
