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

package modulecheck.core

import modulecheck.api.finding.Deletable
import modulecheck.project.ConfigurationName
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject
import java.io.File

data class RedundantDependencyFinding(
  override val dependentPath: String,
  override val buildFile: File,
  override val dependencyProject: McProject,
  val dependencyPath: String,
  override val configurationName: ConfigurationName,
  val from: List<ConfiguredProjectDependency>
) : DependencyFinding("redundant"),
  Deletable {

  override val message: String
    get() = "The dependency is declared as `api` in a dependency module, but also explicitly " +
      "declared in the current module.  This is technically unnecessary if a \"minimalist\" build " +
      "file is desired."

  override val dependencyIdentifier = dependencyPath + fromStringOrEmpty()

  override fun fromStringOrEmpty(): String {

    return if (from.all { dependencyProject.path == it.project.path }) {
      ""
    } else {
      from.joinToString { it.project.path }
    }
  }
}
