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
import modulecheck.parsing.gradle.dsl.ModuleDependencyDeclaration
import modulecheck.parsing.gradle.dsl.createProjectDependencyDeclaration
import modulecheck.parsing.gradle.model.ConfiguredProjectDependency

interface AddsDependency : Fixable {

  val newDependency: ConfiguredProjectDependency

  suspend fun addDependency(): Boolean {
    val token = dependentProject
      .closestDeclarationOrNull(
        newDependency,
        matchPathFirst = false
      ) as? ModuleDependencyDeclaration

    val newDeclaration = token?.copy(
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
}
