/*
 * Copyright (C) 2021-2023 Rick Busarow
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
import com.github.ajalt.mordant.table.grid
import com.github.ajalt.mordant.terminal.Terminal
import modulecheck.api.DepthFinding
import modulecheck.model.sourceset.SourceSetName
import modulecheck.utils.mapLines
import javax.inject.Inject

/**
 * Creates the depth report content as it is printed to the console -- not a file.
 *
 * ex:
 * ```
 * -- ModuleCheck main source set depth results --
 *      depth    modules
 *      0        [:lib1]
 *      1        [:debug1, :lib2]
 *      2        [:app, :debug2]
 * ```
 */
class DepthLogFactory @Inject constructor(private val terminal: Terminal) {
/** */
  fun create(results: List<DepthFinding>): String = buildString {

    appendLine(terminal.theme.warning("-- ModuleCheck main source set depth results --"))

    val depthHeader = "depth"
    val childrenHeader = "modules"

    grid {

      style = style?.plus(terminal.theme.warning) ?: terminal.theme.warning
      whitespace = Whitespace.NORMAL
      overflowWrap = OverflowWrap.NORMAL
      align = TextAlign.NONE
      padding { left = PADDING }

      row(depthHeader, childrenHeader)

      column(0) {
        padding { left = LEADING_PADDING }
      }

      results
        .filter { it.sourceSetName == SourceSetName.MAIN }
        .groupBy { it.depth }
        .toSortedMap()
        .entries
        .forEach { (depth, values) ->

          val paths = values
            .distinctBy { it.dependentPath }
            .sortedBy { it.dependentPath }
            .joinToString(separator = ", ", prefix = "[", postfix = "]") { it.dependentPath.value }

          row(depth.toString(), paths)
        }
    }
      .also { grid -> appendLine(terminal.render(grid).mapLines(String::trimEnd)) }
  }

  companion object {

    private const val LEADING_PADDING = 4
    private const val PADDING = 3
  }
}
