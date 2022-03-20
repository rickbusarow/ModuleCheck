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
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.ModuleDependencyDeclaration
import modulecheck.parsing.gradle.createProjectDependencyDeclaration
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject

data class OverShotDependencyFinding(
  override val dependentProject: McProject,
  override val newDependency: ConfiguredProjectDependency,
  override val oldDependency: ConfiguredProjectDependency,
  override val configurationName: ConfigurationName
) : AbstractProjectDependencyFinding("overshot"),
  ModifiesDependency,
  AddsDependency,
  RemovesDependency {

  override val dependencyProject get() = oldDependency.project
  override val dependencyIdentifier: String get() = newDependency.path.value

  override val message: String
    get() = "The dependency is not used in the source set for which it is configured, but it is " +
      "used in another source set which inherits from the first.  For example, a test-only " +
      "dependency which is declared via `implementation` instead of `testImplementation`."

  override suspend fun fix(): Boolean {

    val token = dependentProject
      .closestDeclarationOrNull(
        newDependency,
        matchPathFirst = false
      ) as? ModuleDependencyDeclaration

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

    return true
  }

  override fun fromStringOrEmpty(): String = ""

  override fun toString(): String {
    return "OverShotDependency(\n" +
      "\tdependentPath='$dependentPath', \n" +
      "\tbuildFile=$buildFile, \n" +
      "\tdependencyProject=$dependencyProject, \n" +
      "\tdependencyIdentifier='$dependencyIdentifier', \n" +
      "\tconfigurationName=$configurationName\n" +
      ")"
  }
}
