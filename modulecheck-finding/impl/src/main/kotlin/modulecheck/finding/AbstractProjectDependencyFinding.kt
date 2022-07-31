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

import modulecheck.finding.Finding.FindingResult
import modulecheck.finding.Finding.Position
import modulecheck.finding.internal.positionOfStatement
import modulecheck.finding.internal.statementOrNullIn
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.parsing.gradle.dsl.BuildFileStatement
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.lazyDeferred
import java.io.File

abstract class AbstractProjectDependencyFinding :
  Problem,
  Fixable,
  Finding,
  DependencyFinding,
  ConfiguredDependencyFinding {

  final override val dependentPath: StringProjectPath get() = dependentProject.path
  final override val buildFile: File get() = dependentProject.buildFile

  override val isSuppressed: LazyDeferred<Boolean> = lazyDeferred {
    dependentProject.getSuppressions()
      .get(findingName)
      .contains(dependency)
  }

  override val positionOrNull: LazyDeferred<Position?> = lazyDeferred {
    val statement = statementOrNull.await()?.declarationText ?: return@lazyDeferred null

    buildFile.readText()
      .positionOfStatement(statement)
  }

  override val statementOrNull: LazyDeferred<BuildFileStatement?> = lazyDeferred {
    dependency.statementOrNullIn(dependentProject)
  }
  override val statementTextOrNull: LazyDeferred<String?> = lazyDeferred {
    statementOrNull.await()?.statementWithSurroundingText
  }

  override suspend fun toResult(fixed: Boolean): FindingResult {
    return FindingResult(
      dependentPath = dependentPath,
      findingName = findingName,
      sourceOrNull = fromStringOrEmpty(),
      configurationName = configurationName.value,
      dependencyIdentifier = dependency.identifier.name,
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
    if (dependency != other.dependency) return false
    if (configurationName != other.configurationName) return false

    return true
  }

  override fun hashCode(): Int {
    var result = findingName.hashCode()
    result = 31 * result + dependency.hashCode()
    result = 31 * result + configurationName.hashCode()
    return result
  }
}
