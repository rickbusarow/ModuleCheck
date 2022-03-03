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

package modulecheck.core.rule.android

import modulecheck.api.finding.Finding
import modulecheck.api.finding.Finding.Position
import modulecheck.api.finding.Fixable
import modulecheck.core.internal.positionOfStatement
import modulecheck.parsing.gradle.Declaration
import modulecheck.project.McProject
import modulecheck.utils.LazyDeferred
import modulecheck.utils.lazyDeferred
import java.io.File

data class UnusedKotlinSyntheticImportsFinding(
  override val dependentProject: McProject,
  override val dependentPath: String,
  override val buildFile: File
) : Finding, Fixable {

  override val message: String
    get() = "Kotlin Android Extensions is enabled, but no synthetic import is being used."

  override val findingName = "disableKotlinAndroidExtensions"

  override val dependencyIdentifier = ""

  override val declarationOrNull: LazyDeferred<Declaration?> = lazyDeferred { null }

  override val statementTextOrNull: LazyDeferred<String?> = lazyDeferred {

    dependentProject.buildFileParser.androidSettings()
      .assignments
      .firstOrNull { it.propertyFullName == "viewBinding" }
      ?.declarationText
  }

  override val positionOrNull: LazyDeferred<Position?> = lazyDeferred {
    val statement = statementTextOrNull.await() ?: return@lazyDeferred null

    val fileText = buildFile.readText()

    fileText.positionOfStatement(statement)
  }

  override suspend fun fix(): Boolean {
    TODO()
  }
}
