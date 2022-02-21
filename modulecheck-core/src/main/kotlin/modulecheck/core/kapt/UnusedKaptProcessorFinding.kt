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

package modulecheck.core.kapt

import modulecheck.api.finding.DependencyFinding
import modulecheck.api.finding.Finding
import modulecheck.api.finding.Finding.Position
import modulecheck.api.finding.Fixable
import modulecheck.api.finding.Problem
import modulecheck.api.finding.RemovesDependency
import modulecheck.core.internal.positionOfStatement
import modulecheck.core.internal.statementOrNullIn
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.Declaration
import modulecheck.project.ConfiguredDependency
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.ExternalDependency
import modulecheck.project.McProject
import modulecheck.utils.LazyDeferred
import modulecheck.utils.lazyDeferred
import java.io.File

data class UnusedKaptProcessorFinding(
  override val dependentProject: McProject,
  override val dependentPath: String,
  override val buildFile: File,
  override val oldDependency: ConfiguredDependency,
  val configurationName: ConfigurationName
) : Finding,
  Problem,
  Fixable,
  DependencyFinding,
  RemovesDependency {

  override val message: String
    get() = "The annotation processor dependency is not used in this module.  " +
      "This can be a significant performance hit."

  override val dependencyIdentifier = when (oldDependency) {
    is ConfiguredProjectDependency -> oldDependency.path
    is ExternalDependency -> oldDependency.name
  }

  override val findingName = "unusedKaptProcessor (${configurationName.value})"

  override val declarationOrNull: LazyDeferred<Declaration?> = lazyDeferred {
    when (oldDependency) {
      is ConfiguredProjectDependency ->
        oldDependency.project
          .statementOrNullIn(dependentProject, oldDependency.configurationName)
      is ExternalDependency ->
        oldDependency
          .statementOrNullIn(dependentProject, oldDependency.configurationName)
    }
  }

  override val statementTextOrNull: LazyDeferred<String?> = lazyDeferred {
    declarationOrNull.await()?.statementWithSurroundingText
  }

  override val positionOrNull: LazyDeferred<Position?> = lazyDeferred {
    val statement = declarationOrNull.await()?.declarationText ?: return@lazyDeferred null

    buildFile.readText()
      .positionOfStatement(statement)
  }
}
