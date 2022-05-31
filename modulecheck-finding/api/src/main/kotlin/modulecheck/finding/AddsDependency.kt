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

import modulecheck.finding.internal.addDependency
import modulecheck.finding.internal.closestDeclarationOrNull
import modulecheck.model.dependency.ConfiguredDependency
import modulecheck.model.dependency.ExternalDependency
import modulecheck.model.dependency.ProjectDependency
import modulecheck.parsing.gradle.dsl.ExternalDependencyDeclaration
import modulecheck.parsing.gradle.dsl.ModuleDependencyDeclaration
import modulecheck.parsing.gradle.dsl.createDependencyDeclaration

interface AddsDependency : Fixable {

  /** The dependency to be added */
  val newDependency: ConfiguredDependency

  suspend fun addDependency(): Boolean {

    val token = dependentProject
      .closestDeclarationOrNull(
        newDependency,
        matchPathFirst = false
      )

    val newDeclaration = when (val newDependency = newDependency) {
      is ProjectDependency -> {

        (token as? ModuleDependencyDeclaration)?.copy(
          newConfigName = newDependency.configurationName,
          newModulePath = newDependency.path,
          testFixtures = newDependency.isTestFixture
        )
          ?: dependentProject.createDependencyDeclaration(
            configurationName = newDependency.configurationName,
            identifier = newDependency.path,
            isTestFixtures = newDependency.isTestFixture
          ) as ModuleDependencyDeclaration
      }

      is ExternalDependency -> {

        (token as? ExternalDependencyDeclaration)?.copy(
          newConfigName = newDependency.configurationName,
          newCoordinates = newDependency.mavenCoordinates,
          testFixtures = newDependency.isTestFixture
        )
          ?: dependentProject.createDependencyDeclaration(
            configurationName = newDependency.configurationName,
            identifier = newDependency.mavenCoordinates,
            isTestFixtures = newDependency.isTestFixture
          ) as ModuleDependencyDeclaration
      }
    }

    dependentProject.addDependency(newDependency, newDeclaration, token)

    return true
  }
}
