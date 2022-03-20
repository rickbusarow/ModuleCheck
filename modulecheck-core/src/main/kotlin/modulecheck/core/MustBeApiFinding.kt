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

import modulecheck.api.finding.AddsDependency
import modulecheck.api.finding.ModifiesDependency
import modulecheck.api.finding.RemovesDependency
import modulecheck.api.finding.addDependency
import modulecheck.api.finding.closestDeclarationOrNull
import modulecheck.api.finding.removeDependencyWithDelete
import modulecheck.core.internal.statementOrNullIn
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.Declaration
import modulecheck.parsing.gradle.ModuleDependencyDeclaration
import modulecheck.parsing.gradle.createProjectDependencyDeclaration
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject
import modulecheck.utils.LazyDeferred
import modulecheck.utils.lazyDeferred

data class MustBeApiFinding(
  override val dependentProject: McProject,
  override val newDependency: ConfiguredProjectDependency,
  override val oldDependency: ConfiguredProjectDependency,
  override val configurationName: ConfigurationName,
  val source: ConfiguredProjectDependency?
) : AbstractProjectDependencyFinding("mustBeApi"),
  ModifiesDependency,
  AddsDependency,
  RemovesDependency {

  override val dependencyProject get() = oldDependency.project

  override val message: String
    get() = "The dependency should be declared via an `api` configuration, since it provides " +
      "a declaration which is referenced in this module's public API."

  override val dependencyIdentifier = dependencyProject.path.value + fromStringOrEmpty()

  override val declarationOrNull: LazyDeferred<Declaration?> = lazyDeferred {
    super.declarationOrNull.await()
      ?: source?.project
        ?.statementOrNullIn(dependentProject, configurationName)
  }

  override fun fromStringOrEmpty(): String {
    return if (dependencyProject.path == source?.project?.path) {
      ""
    } else {
      "${source?.project?.path?.value}"
    }
  }

  override suspend fun fix(): Boolean {

    val token = dependentProject
      .closestDeclarationOrNull(
        newDependency,
        matchPathFirst = true
      ) as? ModuleDependencyDeclaration

    val oldDeclaration = declarationOrNull.await()

    val newDeclaration = token?.replace(
      newConfigName = newDependency.configurationName,
      newModulePath = newDependency.path,
      testFixtures = newDependency.isTestFixture
    )
      ?: dependentProject.createProjectDependencyDeclaration(
        configurationName = newDependency.configurationName,
        projectPath = newDependency.path,
        isTestFixtures = newDependency.isTestFixture
      )

    dependentProject.addDependency(newDependency, newDeclaration, token)

    if (oldDeclaration != null) {
      dependentProject.removeDependencyWithDelete(oldDeclaration, oldDependency)
    }

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
      |)
    """.trimMargin()
  }
}
