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

import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.Declaration
import modulecheck.parsing.gradle.ProjectPath
import modulecheck.project.ConfiguredDependency
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.ExternalDependency
import modulecheck.project.McProject
import modulecheck.rule.RuleName
import modulecheck.rule.finding.ConfigurationFinding
import modulecheck.rule.finding.DependencyFinding
import modulecheck.rule.finding.Finding
import modulecheck.rule.finding.Finding.Position
import modulecheck.rule.finding.Fixable
import modulecheck.rule.finding.Problem
import modulecheck.rule.finding.RemovesDependency
import modulecheck.rule.finding.internal.positionOfStatement
import modulecheck.rule.finding.internal.statementOrNullIn
import modulecheck.utils.LazyDeferred
import modulecheck.utils.lazyDeferred
import java.io.File

data class UnusedKaptProcessorFinding(
  override val ruleName: RuleName,
  override val dependentProject: McProject,
  override val dependentPath: ProjectPath.StringProjectPath,
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

  override val declarationOrNull: LazyDeferred<Declaration?> = lazyDeferred {
    when (oldDependency) {
      is ConfiguredProjectDependency ->
        oldDependency.statementOrNullIn(dependentProject)
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
