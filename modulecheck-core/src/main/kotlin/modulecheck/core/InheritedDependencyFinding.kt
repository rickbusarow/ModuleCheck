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
import modulecheck.api.finding.Finding.Position
import modulecheck.api.finding.addDependency
import modulecheck.api.finding.closestDeclarationOrNull
import modulecheck.core.internal.positionIn
import modulecheck.core.internal.statementOrNullIn
import modulecheck.parsing.gradle.Declaration
import modulecheck.parsing.gradle.ModuleDependencyDeclaration
import modulecheck.parsing.gradle.createProjectDependencyDeclaration
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject
import modulecheck.utils.LazyDeferred
import modulecheck.utils.lazyDeferred

data class InheritedDependencyFinding(
  override val dependentProject: McProject,
  override val newDependency: ConfiguredProjectDependency,
  val source: ConfiguredProjectDependency
) : AbstractProjectDependencyFinding("inheritedDependency"),
  AddsDependency,
  Comparable<InheritedDependencyFinding> {

  override val message: String
    get() = "Transitive dependencies which are directly referenced should be declared in this module."

  override val dependencyIdentifier get() = newDependency.path.value + fromStringOrEmpty()
  override val dependencyProject get() = newDependency.project
  override val configurationName get() = newDependency.configurationName

  override val declarationOrNull: LazyDeferred<Declaration?> = lazyDeferred {
    source.project
      .statementOrNullIn(dependentProject, source.configurationName)
  }
  override val positionOrNull: LazyDeferred<Position?> = lazyDeferred {
    source.project.positionIn(dependentProject, source.configurationName)
  }

  override fun fromStringOrEmpty(): String {
    return if (dependencyProject.path == source.project.path) {
      ""
    } else {
      source.project.path.value
    }
  }

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

  override fun compareTo(other: InheritedDependencyFinding): Int {

    return compareBy<InheritedDependencyFinding>(
      { it.configurationName },
      { it.source.isTestFixture },
      { it.newDependency.path }
    ).compare(this, other)
  }
}
