/*
 * Copyright (C) 2021-2023 Rick Busarow
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
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.ConfiguredDependency
import modulecheck.parsing.gradle.dsl.BuildFileStatement
import modulecheck.project.McProject
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.lazyDeferred

/**
 * Represents a [ConfiguredDependency] which is unused in the
 * [SourceSet][modulecheck.model.dependency.McSourceSet] to which it's
 * added, but *is* used in another source set downstream. For instance, a
 * dependency is overshot if it's added to `main`, but only used in `test`.
 *
 * @property dependentProject the [McProject] declaring the dependency
 * @property newDependency the [ConfiguredDependency] which should be added
 * @property oldDependency the [ConfiguredDependency] which adds the unused dependency
 * @since 0.12.0
 */
data class OverShotDependency(
  val dependentProject: McProject,
  val newDependency: ConfiguredDependency,
  val oldDependency: ConfiguredDependency
) {
  /**
   * Converts the `OverShotDependency` to an [OverShotDependencyFinding].
   *
   * @return the finding matching this [OverShotDependency]
   * @since 0.12.0
   */
  fun toFinding(): OverShotDependencyFinding = OverShotDependencyFinding(
    dependentProject = dependentProject,
    newDependency = newDependency,
    oldDependency = oldDependency
  )
}

data class OverShotDependencyFinding(
  override val dependentProject: McProject,
  override val newDependency: ConfiguredDependency,
  val oldDependency: ConfiguredDependency
) : AbstractProjectDependencyFinding(),
  AddsDependency {

  override val findingName: FindingName = NAME

  override val configurationName: ConfigurationName = newDependency.configurationName

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

  override val dependency: ConfiguredDependency get() = newDependency
  override val dependencyIdentifier: String get() = newDependency.identifier.name

  override val message: String
    get() = "The dependency is not used in the source set for which it is configured, but it is " +
      "used in another source set which inherits from the first.  For example, a test-only " +
      "dependency which is declared via `implementation` instead of `testImplementation`."

  override fun fromStringOrEmpty(): String = ""

  override fun toString(): String {
    return """
        |OverShotDependency(
        |  dependentPath='$dependentPath',
        |  buildFile=$buildFile,
        |  dependency=$dependency,
        |  dependencyIdentifier='$dependencyIdentifier',
        |  configurationName=$configurationName
        |)
      """.replaceIndentByMargin()
  }

  companion object {
    val NAME: FindingName = FindingName("overshot-dependency")
  }
}
