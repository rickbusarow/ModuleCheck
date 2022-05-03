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

import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.Declaration
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject
import modulecheck.rule.RuleName
import modulecheck.rule.finding.AddsDependency
import modulecheck.rule.finding.Finding.Position
import modulecheck.rule.finding.internal.positionOfStatement
import modulecheck.rule.finding.internal.statementOrNullIn
import modulecheck.utils.LazyDeferred
import modulecheck.utils.lazyDeferred

data class OverShotDependency(
  val dependentProject: McProject,
  val newDependency: ConfiguredProjectDependency,
  val oldDependency: ConfiguredProjectDependency,
  val configurationName: ConfigurationName
) {
  fun toFinding(ruleName: RuleName): OverShotDependencyFinding = OverShotDependencyFinding(
    ruleName = ruleName,
    dependentProject = dependentProject,
    newDependency = newDependency,
    oldDependency = oldDependency,
    configurationName = configurationName
  )
}

data class OverShotDependencyFinding(
  override val ruleName: RuleName,
  override val dependentProject: McProject,
  override val newDependency: ConfiguredProjectDependency,
  val oldDependency: ConfiguredProjectDependency,
  override val configurationName: ConfigurationName
) : AbstractProjectDependencyFinding(),
  AddsDependency {

  override val declarationOrNull: LazyDeferred<Declaration?>
    // intentionally look this up every time, since the declaration doesn't exist at first
    get() = lazyDeferred {
      dependency.statementOrNullIn(dependentProject)
    }

  override val positionOrNull: LazyDeferred<Position?>
    get() = lazyDeferred {
      val statement = declarationOrNull.await()?.declarationText
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
