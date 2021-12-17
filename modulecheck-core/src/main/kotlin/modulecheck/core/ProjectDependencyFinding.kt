/*
 * Copyright (C) 2021 Rick Busarow
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

import modulecheck.api.finding.Finding
import modulecheck.api.finding.Finding.FindingResult
import modulecheck.api.finding.Fixable
import modulecheck.api.finding.Problem
import modulecheck.core.internal.positionOfStatement
import modulecheck.core.internal.statementOrNullIn
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.ModuleDependencyDeclaration
import modulecheck.project.McProject
import java.io.File

abstract class ProjectDependencyFinding(
  override val findingName: String
) : Problem,
  Fixable,
  Finding {

  final override val dependentPath: String get() = dependentProject.path
  final override val buildFile: File get() = dependentProject.buildFile

  abstract val dependencyProject: McProject
  abstract val configurationName: ConfigurationName

  override val positionOrNull by lazy {
    val statement = declarationOrNull?.declarationText ?: return@lazy null

    buildFile.readText()
      .positionOfStatement(statement)
  }

  override val declarationOrNull: ModuleDependencyDeclaration? by lazy {
    dependencyProject
      .statementOrNullIn(dependentProject, configurationName)
  }
  override val statementTextOrNull: String? by lazy {
    declarationOrNull?.statementWithSurroundingText
  }

  override fun toResult(fixed: Boolean): FindingResult {
    return FindingResult(
      dependentPath = dependentPath,
      problemName = findingName,
      sourceOrNull = fromStringOrEmpty(),
      dependencyPath = dependencyProject.path,
      positionOrNull = positionOrNull,
      buildFile = buildFile,
      message = message,
      fixed = fixed
    )
  }

  abstract fun fromStringOrEmpty(): String

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ProjectDependencyFinding) return false

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
