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
import modulecheck.finding.internal.positionOfStatement
import modulecheck.finding.internal.statementOrNullIn
import modulecheck.parsing.gradle.dsl.BuildFileStatement
import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.parsing.gradle.model.ConfiguredProjectDependency
import modulecheck.project.McProject
import modulecheck.utils.LazyDeferred
import modulecheck.utils.lazyDeferred

data class OverShotDependency(
  val dependentProject: McProject,
  val newDependency: ConfiguredProjectDependency,
  val oldDependency: ConfiguredProjectDependency,
  val configurationName: ConfigurationName
) {
  fun toFinding(findingName: FindingName): OverShotDependencyFinding = OverShotDependencyFinding(
    findingName = findingName,
    dependentProject = dependentProject,
    newDependency = newDependency,
    oldDependency = oldDependency,
    configurationName = configurationName
  )
}

data class OverShotDependencyFinding(
  override val findingName: FindingName,
  override val dependentProject: McProject,
  override val newDependency: ConfiguredProjectDependency,
  val oldDependency: ConfiguredProjectDependency,
  override val configurationName: ConfigurationName
) : AbstractProjectDependencyFinding(),
  AddsDependency {

  override val statementOrNull: LazyDeferred<BuildFileStatement?>
    // intentionally look this up every time, since the declaration doesn't exist at first
    get() = lazyDeferred {
      dependency.statementOrNullIn(dependentProject)
    }

  override val positionOrNull: LazyDeferred<Position?>
    get() = lazyDeferred {
      val statement = statementOrNull.await()?.declarationText
        ?: oldDependency.statementOrNullIn(dependentProject)?.declarationText
        ?: return@lazyDeferred null

      buildFile.readText()
        .positionOfStatement(statement)
    }

  override val dependency get() = newDependency
  override val dependencyIdentifier: String get() = newDependency.path.value

  override val message: String
    get() = "The dependency is not used in the source set for which it is configured, but it is " +
      "used in another source set which inherits from the first.  For example, a test-only " +
      "dependency which is declared via `implementation` instead of `testImplementation`."

  override fun fromStringOrEmpty(): String = ""

  override fun toString(): String {
    return "OverShotDependency(\n" +
      "\tdependentPath='$dependentPath', \n" +
      "\tbuildFile=$buildFile, \n" +
      "\tdependency=$dependency, \n" +
      "\tdependencyIdentifier='$dependencyIdentifier', \n" +
      "\tconfigurationName=$configurationName\n" +
      ")"
  }
}
