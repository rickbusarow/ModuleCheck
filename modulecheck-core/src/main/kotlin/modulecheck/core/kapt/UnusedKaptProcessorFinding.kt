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

import modulecheck.api.finding.ConfigurationFinding
import modulecheck.api.finding.DependencyFinding
import modulecheck.api.finding.Finding
import modulecheck.api.finding.Finding.Position
import modulecheck.api.finding.Fixable
import modulecheck.api.finding.Problem
import modulecheck.api.finding.RemovesDependency
import modulecheck.api.finding.internal.positionOfStatement
import modulecheck.api.finding.internal.statementOrNullIn
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.Declaration
import modulecheck.parsing.gradle.ProjectPath
import modulecheck.project.ConfiguredDependency
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.ExternalDependency
import modulecheck.project.McProject
import modulecheck.utils.LazyDeferred
import modulecheck.utils.lazyDeferred
import java.io.File

data class UnusedKaptProcessorFinding(
	override val subjectProject: McProject,
	override val subjectPath: ProjectPath.StringProjectPath,
	override val buildFile: File,
	override val oldDependency: ConfiguredDependency,
	override val configurationName: ConfigurationName
) : Finding,
  Problem,
  Fixable,
  DependencyFinding,
  ConfigurationFinding,
  RemovesDependency {

  override val message: String
    get() = "The annotation processor dependency is not used in this module.  " +
      "This can be a significant performance hit."

  override val dependencyIdentifier = when (oldDependency) {
    is ConfiguredProjectDependency -> oldDependency.path.value
    is ExternalDependency -> oldDependency.name
  }

  override val findingName = "unusedKaptProcessor"

  override val declarationOrNull: LazyDeferred<Declaration?> = lazyDeferred {
    when (oldDependency) {
      is ConfiguredProjectDependency ->
        oldDependency.statementOrNullIn(subjectProject)
      is ExternalDependency ->
        oldDependency
          .statementOrNullIn(subjectProject, oldDependency.configurationName)
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
