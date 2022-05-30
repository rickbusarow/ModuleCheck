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
import modulecheck.model.dependency.ConfiguredDependency
import modulecheck.model.dependency.ConfiguredProjectDependency
import modulecheck.model.dependency.ExternalDependency
import modulecheck.parsing.gradle.dsl.BuildFileStatement
import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.parsing.gradle.model.ProjectPath
import modulecheck.project.McProject
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.lazyDeferred
import java.io.File

data class UnusedKaptProcessorFinding(
  override val findingName: FindingName,
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

  override val statementOrNull: LazyDeferred<BuildFileStatement?> = lazyDeferred {
    when (oldDependency) {
      is ConfiguredProjectDependency ->
        oldDependency.statementOrNullIn(dependentProject)

      is ExternalDependency ->
        oldDependency
          .statementOrNullIn(dependentProject, oldDependency.configurationName)
    }
  }

  override val statementTextOrNull: LazyDeferred<String?> = lazyDeferred {
    statementOrNull.await()?.statementWithSurroundingText
  }

  override val positionOrNull: LazyDeferred<Position?> = lazyDeferred {
    val statement = statementOrNull.await()?.declarationText ?: return@lazyDeferred null

    buildFile.readText()
      .positionOfStatement(statement)
  }
}
