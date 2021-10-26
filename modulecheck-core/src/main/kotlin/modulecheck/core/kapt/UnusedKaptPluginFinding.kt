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

package modulecheck.core.kapt

import modulecheck.api.Finding
import modulecheck.api.Finding.LogElement
import modulecheck.api.Finding.Position
import modulecheck.api.Fixable
import modulecheck.core.rule.KAPT_PLUGIN_FUN
import modulecheck.core.rule.KAPT_PLUGIN_ID
import java.io.File

interface UnusedKaptFinding : Finding, Fixable

data class UnusedKaptPluginFinding(
  override val dependentPath: String,
  override val buildFile: File
) : UnusedKaptFinding {

  override val dependencyIdentifier = KAPT_PLUGIN_ID

  override val problemName = "unusedKaptPlugin"

  override fun logElement(): LogElement {
    return LogElement(
      dependentPath = dependentPath,
      problemName = problemName,
      sourceOrNull = null,
      dependencyPath = dependencyIdentifier,
      positionOrNull = positionOrNull,
      buildFile = buildFile
    )
  }

  override val positionOrNull: Position? by lazy {
    val text = buildFile
      .readText()

    val lines = text.lines()

    val row = lines
      .indexOfFirst { line ->
        line.contains("id(\"$KAPT_PLUGIN_ID\")") ||
          line.contains(KAPT_PLUGIN_FUN) ||
          line.contains("plugin = \"$KAPT_PLUGIN_ID\")")
      }

    if (row < 0) return@lazy null

    val col = lines[row]
      .indexOfFirst { it != ' ' }

    Position(row + 1, col + 1)
  }
}
