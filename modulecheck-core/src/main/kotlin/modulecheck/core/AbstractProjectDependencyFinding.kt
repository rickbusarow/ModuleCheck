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

import modulecheck.api.finding.DependencyFinding
import modulecheck.api.finding.Finding
import modulecheck.api.finding.Finding.FindingResult
import modulecheck.api.finding.Finding.Position
import modulecheck.api.finding.Fixable
import modulecheck.api.finding.Problem
import modulecheck.api.finding.ProjectDependencyFinding
import modulecheck.core.internal.positionOfStatement
import modulecheck.core.internal.statementOrNullIn
import modulecheck.parsing.gradle.Declaration
import modulecheck.utils.LazyDeferred
import modulecheck.utils.lazyDeferred
import java.io.File

abstract class AbstractProjectDependencyFinding(
  override val findingName: String
) : Problem,
  Fixable,
  Finding,
  DependencyFinding,
  ProjectDependencyFinding {

  final override val dependentPath: String get() = dependentProject.path
  final override val buildFile: File get() = dependentProject.buildFile

  override val positionOrNull: LazyDeferred<Position?> = lazyDeferred {
    val statement = declarationOrNull.await()?.declarationText ?: return@lazyDeferred null

    buildFile.readText()
      .positionOfStatement(statement)
  }

  override val declarationOrNull: LazyDeferred<Declaration?> = lazyDeferred {
    dependencyProject
      .statementOrNullIn(dependentProject, configurationName)
  }
  override val statementTextOrNull: LazyDeferred<String?> = lazyDeferred {
    declarationOrNull.await()?.statementWithSurroundingText
  }

  override suspend fun toResult(fixed: Boolean): FindingResult {
    return FindingResult(
      dependentPath = dependentPath,
      problemName = findingName,
      sourceOrNull = fromStringOrEmpty(),
      dependencyPath = dependencyProject.path,
      positionOrNull = positionOrNull.await(),
      buildFile = buildFile,
      message = message,
      fixed = fixed
    )
  }

  abstract fun fromStringOrEmpty(): String

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AbstractProjectDependencyFinding) return false

    if (findingName != other.findingName) return false
    if (dependencyProject != other.dependencyProject) return false
    if (configurationName != other.configurationName) return false

    return true
  }

  override fun hashCode(): Int {
    var result = findingName.hashCode()
    result = 31 * result + dependencyProject.hashCode()
    result = 31 * result + configurationName.hashCode()
    return result
  }
}
