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

package modulecheck.api.finding

import modulecheck.api.finding.Finding.Position
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.Declaration
import modulecheck.parsing.gradle.ProjectPath.StringProjectPath
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject
import modulecheck.utils.LazyDeferred
import java.io.File

// sealed interface FindingResult {
//
//   val subjectPath: StringProjectPath
//   val buildFile: File
//
//   val problemName: String
//   val sourceOrNull: String?
//   val configurationName: String
//   val dependencyIdentifier: String
//   val positionOrNull: Position?
//   val message: String
//   val fixed: Boolean
//
//   val filePathString: String
//     get() = "${buildFile.path}: ${positionOrNull?.logString().orEmpty()}"
// }
//
// interface HasOldPosition {
//   val oldPositionOrNull: LazyDeferred<Position?>
// }
//
// interface HasNewPosition {
//   val newPositionOrNull: LazyDeferred<Position?>
// }

interface Finding {


  val subjectProject: McProject
  val subjectPath: StringProjectPath

  val findingName: String

  val message: String
  val buildFile: File

  val dependencyIdentifier: String

  @Deprecated("remove")
  val positionOrNull: LazyDeferred<Position?>

  suspend fun toResult(fixed: Boolean): FindingResult

  data class Position(
    val row: Int,
    val column: Int
  ) : Comparable<Position> {
    fun logString(): String = "($row, $column): "
    override fun compareTo(other: Position): Int {
      return row.compareTo(other.row)
    }
  }

  data class FindingResult(
    val dependentPath: StringProjectPath,
    val problemName: String,
    val sourceOrNull: String?,
    val configurationName: String,
    val dependencyIdentifier: String,
    val positionOrNull: Position?,
    val buildFile: File,
    val message: String,
    val fixed: Boolean
  ) {
    val filePathString: String = "${buildFile.path}: ${positionOrNull?.logString().orEmpty()}"
  }
}

interface FindingWithSource : Finding {
  val source: FindingSource
}

data class FindingSource(
  val buildFile: File,
  val oldPositionOrNull: LazyDeferred<Position?>
)

interface DependencyFinding {

  val declarationOrNull: LazyDeferred<Declaration?>
  val statementTextOrNull: LazyDeferred<String?>
}

interface ConfigurationFinding {

  val configurationName: ConfigurationName
}

interface ProjectDependencyFinding : ConfigurationFinding {
  val dependency: ConfiguredProjectDependency

  override val configurationName: ConfigurationName
}
