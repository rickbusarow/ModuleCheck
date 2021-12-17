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
import modulecheck.api.finding.Finding.Position
import modulecheck.api.finding.addDependency
import modulecheck.core.internal.positionIn
import modulecheck.core.internal.statementOrNullIn
import modulecheck.parsing.gradle.ModuleDependencyDeclaration
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject

data class InheritedDependencyFinding(
  override val dependentProject: McProject,
  override val newDependency: ConfiguredProjectDependency,
  val source: ConfiguredProjectDependency
) : ProjectDependencyFinding("inheritedDependency"),
  AddsDependency,
  Comparable<InheritedDependencyFinding> {

  override val message: String
    get() = "Transitive dependencies which are directly referenced should be declared in this module."

  override val dependencyIdentifier get() = newDependency.path + fromStringOrEmpty()
  override val dependencyProject get() = newDependency.project
  override val configurationName get() = newDependency.configurationName

  override val declarationOrNull: ModuleDependencyDeclaration? by lazy {
    source.project
      .statementOrNullIn(dependentProject, source.configurationName)
  }
  override val positionOrNull: Position? by lazy {
    source.project.positionIn(dependentProject, source.configurationName)
  }

  override fun fromStringOrEmpty(): String {
    return if (dependencyProject.path == source.project.path) {
      ""
    } else {
      source.project.path
    }
  }

  override fun fix(): Boolean = synchronized(buildFile) {

    val oldDeclaration = declarationOrNull ?: return false

    val newDeclaration = oldDeclaration.replace(
      configName = newDependency.configurationName,
      modulePath = newDependency.path,
      testFixtures = newDependency.isTestFixture
    )

    dependentProject.addDependency(newDependency, newDeclaration, oldDeclaration)

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
