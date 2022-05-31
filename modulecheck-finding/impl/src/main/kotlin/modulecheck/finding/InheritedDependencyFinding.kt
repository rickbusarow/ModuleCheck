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

import modulecheck.finding.Finding.Position
import modulecheck.finding.internal.positionIn
import modulecheck.finding.internal.statementOrNullIn
import modulecheck.model.dependency.ProjectDependency
import modulecheck.parsing.gradle.dsl.BuildFileStatement
import modulecheck.project.McProject
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.lazyDeferred

data class InheritedDependencyFinding(
  override val findingName: FindingName,
  override val dependentProject: McProject,
  override val newDependency: ProjectDependency,
  val source: ProjectDependency
) : AbstractProjectDependencyFinding(),
  AddsDependency,
  Comparable<InheritedDependencyFinding> {

  override val message: String
    get() = "Transitive dependencies which are directly referenced should be declared in this module."

  override val dependencyIdentifier get() = newDependency.path.value + fromStringOrEmpty()
  override val dependency get() = newDependency

  override val configurationName get() = newDependency.configurationName

  override val statementOrNull: LazyDeferred<BuildFileStatement?> = lazyDeferred {
    source.statementOrNullIn(dependentProject)
  }
  override val positionOrNull: LazyDeferred<Position?> = lazyDeferred {
    source.positionIn(dependentProject)
  }

  override fun fromStringOrEmpty(): String {
    return if (dependency.path == source.path) {
      ""
    } else {
      source.path.value
    }
  }

  override fun compareTo(other: InheritedDependencyFinding): Int {

    return compareBy<InheritedDependencyFinding>(
      { it.configurationName },
      { it.source.isTestFixture },
      { it.newDependency.path }
    ).compare(this, other)
  }
}
