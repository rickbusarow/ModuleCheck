/*
 * Copyright (C) 2021-2025 Rick Busarow
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

package modulecheck.reporting.console

import com.github.ajalt.mordant.rendering.OverflowWrap
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.Whitespace
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.grid
import com.github.ajalt.mordant.terminal.Terminal
import modulecheck.finding.Finding
import java.util.Locale
import javax.inject.Inject

/**
 * Creates the main report as it is printed in the console.
 *
 * ex:
 * ```
 * -- ModuleCheck results --
 *     :lib2
 *            configuration     dependency    name           source    build file
 *         ✔  implementation    :lib1         must-be-api              [...]/lib2/build.gradle.kts: (6, 3):
 * ```
 */
class ReportFactory @Inject constructor(private val terminal: Terminal) {

  /** */
  fun create(results: List<Finding.FindingResult>): String = buildString {

    val theme = terminal.theme

    appendLine("-- ModuleCheck results --")

    val entries = results.groupBy { it.dependentPath.value.lowercase(Locale.getDefault()) }
      .entries
      .sortedBy { it.key }

    for (entry in entries) {

      val values = entry.value
      val path = values.first().dependentPath

      val sortedValues = values.sortedWith(
        compareBy(
          { !it.fixed },
          { it.dependencyIdentifier },
          { it.positionOrNull },
          { it.findingName.id },
          { it.configurationName },
          { it.sourceOrNull },
          { it.toString() }
        )
      )

      appendLine("    ${path.value}")
      val grid = grid {

        cellBorders = Borders.NONE
        // borderType = BorderType.DOUBLE

        whitespace = Whitespace.PRE
        overflowWrap = OverflowWrap.NORMAL

        padding {
          left = PADDING
          right = 1
        }

        row("   ", "configuration", "dependency", "name", "source", "build file")

        column(0) {
          align = TextAlign.RIGHT
          padding {
            right = 0
          }
        }
        column(1) {
          padding {
            left = 2
            right = 1
          }
        }

        align = TextAlign.LEFT

        for (result in sortedValues) {
          val icon = if (result.fixed) theme.success(FIXED) else theme.danger(ERROR)

          val rowColor = if (result.fixed) theme.warning.color else theme.danger.color

          row(
            icon,
            result.configurationName,
            result.dependencyIdentifier,
            result.findingName.id,
            result.sourceOrNull.orEmpty(),
            result.filePathString
          ) {
            style(color = rowColor)
          }
        }
      }

      appendLine(terminal.render(grid).prependIndent("      "))

      if (entry != entries.last()) {
        appendLine()
      }
    }
  }

  companion object {
    /** */
    const val FIXED: String = "✔"

    /** */
    const val ERROR: String = "X"

    /** */
    const val PADDING: Int = 3
  }
}
