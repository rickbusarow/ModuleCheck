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
import modulecheck.finding.internal.positionOf
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.ProjectPath
import modulecheck.parsing.gradle.dsl.BuildFileStatement
import modulecheck.project.McProject
import modulecheck.utils.existsOrNull
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.lazyDeferred
import java.io.File

data class CouldUseAnvilFinding(
  override val findingName: FindingName,
  override val dependentProject: McProject,
  override val buildFile: File
) : Finding, Fixable {

  override val dependentPath: ProjectPath.StringProjectPath = dependentProject.projectPath

  override val message: String
    get() = "Dagger's compiler could be replaced with Anvil's factory generation for faster builds."

  override val dependencyIdentifier = "com.google.dagger:dagger-compiler"

  override val statementOrNull: LazyDeferred<BuildFileStatement?> = lazyDeferred { null }

  override val positionOrNull: LazyDeferred<Position?> = lazyDeferred {

    val statement = statementTextOrNull.await() ?: return@lazyDeferred null

    buildFile
      .existsOrNull()
      ?.readText()
      ?.lines()
      ?.positionOf(statement, ConfigurationName.kapt)
  }

  override val statementTextOrNull: LazyDeferred<String?> = lazyDeferred { null }
}
