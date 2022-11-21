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

import modulecheck.finding.internal.statementOrNullIn
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.ProjectDependency
import modulecheck.parsing.gradle.dsl.BuildFileStatement
import modulecheck.project.McProject
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.lazyDeferred

data class MustBeApiFinding(
  override val findingName: FindingName,
  override val dependentProject: McProject,
  override val newDependency: ProjectDependency,
  override val oldDependency: ProjectDependency,
  override val configurationName: ConfigurationName,
  val source: ProjectDependency?
) : AbstractProjectDependencyFinding(),
  ModifiesProjectDependency,
  AddsDependency,
  RemovesDependency {

  override val dependency: ProjectDependency
    get() = oldDependency

  override val message: String
    get() = "The dependency should be declared via an `api` configuration, since it provides " +
      "a declaration which is referenced in this module's public API."

  override val dependencyIdentifier: String = dependency.path.value + fromStringOrEmpty()

  override val statementOrNull: LazyDeferred<BuildFileStatement?> = lazyDeferred {
    super.statementOrNull.await()
      ?: source?.statementOrNullIn(dependentProject)
  }

  override fun fromStringOrEmpty(): String {
    return if (dependency.path == source?.path) {
      ""
    } else {
      "${source?.path?.value}"
    }
  }

  override fun toString(): String {
    return """MustBeApiFinding(
      |   dependentPath='$dependentPath',
      |   buildFile=$buildFile,
      |   dependency=$dependency,
      |   configurationName=$configurationName,
      |   source=$source,
      |   dependencyIdentifier='$dependencyIdentifier'
      |)
    """.trimMargin()
  }
}
