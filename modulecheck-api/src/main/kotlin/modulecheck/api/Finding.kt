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

package modulecheck.api

import modulecheck.parsing.ModuleDependencyDeclaration
import java.io.File

interface Finding {

  val problemName: String
  val dependentPath: String
  val message: String
  val buildFile: File

  val statementOrNull: ModuleDependencyDeclaration? get() = null
  val statementTextOrNull: String? get() = null
  val positionOrNull: Position?

  fun toResult(fixed: Boolean): FindingResult {
    return FindingResult(
      dependentPath = dependentPath,
      problemName = problemName,
      sourceOrNull = null,
      dependencyPath = "",
      positionOrNull = positionOrNull,
      buildFile = buildFile,
      message = message,
      fixed = fixed
    )
  }

  fun shouldSkip(): Boolean = statementOrNull?.suppressed
    ?.contains(problemName)
    ?: false

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
    val dependentPath: String,
    val problemName: String,
    val sourceOrNull: String?,
    val dependencyPath: String,
    val positionOrNull: Position?,
    val buildFile: File,
    val message: String,
    val fixed: Boolean
  ) {
    val filePathString: String = "${buildFile.path}: ${positionOrNull?.logString().orEmpty()}"
  }
}
