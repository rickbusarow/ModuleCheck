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

import modulecheck.api.finding.AddsDependency
import modulecheck.api.finding.RemovesDependency
import modulecheck.api.finding.addDependency
import modulecheck.api.finding.removeDependencyWithDelete
import modulecheck.core.internal.statementOrNullIn
import modulecheck.parsing.ModuleDependencyDeclaration
import modulecheck.project.ConfigurationName
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject

data class MustBeApiFinding(
  override val dependentProject: McProject,
  override val newDependency: ConfiguredProjectDependency,
  override val oldDependency: ConfiguredProjectDependency,
  override val configurationName: ConfigurationName,
  val source: ConfiguredProjectDependency?
) : ProjectDependencyFinding("mustBeApi"),
  AddsDependency,
  RemovesDependency {

  override val dependencyProject get() = oldDependency.project

  override val message: String
    get() = "The dependency should be declared via an `api` configuration, since it provides " +
      "a declaration which is referenced in this module's public API."

  override val dependencyIdentifier = dependencyProject.path + fromStringOrEmpty()

  override val declarationOrNull: ModuleDependencyDeclaration? by lazy {
    super<ProjectDependencyFinding>.declarationOrNull
      ?: source?.project
        ?.statementOrNullIn(buildFile, configurationName)
  }

  override fun fromStringOrEmpty(): String {
    return if (dependencyProject.path == source?.project?.path) {
      ""
    } else {
      "${source?.project?.path}"
    }
  }

  override fun fix(): Boolean = synchronized(buildFile) {

    val oldDeclaration = declarationOrNull ?: return false

    val newDeclaration = oldDeclaration.replace(
      configName = newDependency.configurationName,
      testFixtures = newDependency.isTestFixture
    )

    dependentProject.addDependency(newDependency, newDeclaration, oldDeclaration)
    dependentProject.removeDependencyWithDelete(oldDependency, oldDeclaration)

    return true
  }

  override fun toString(): String {
    return """MustBeApiFinding(
      |   dependentPath='$dependentPath',
      |   buildFile=$buildFile,
      |   dependencyProject=$dependencyProject,
      |   configurationName=$configurationName,
      |   source=$source,
      |   dependencyIdentifier='$dependencyIdentifier'
      |)""".trimMargin()
  }
}
