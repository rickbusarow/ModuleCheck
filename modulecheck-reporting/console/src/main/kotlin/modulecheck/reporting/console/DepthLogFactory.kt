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

package modulecheck.reporting.console

import modulecheck.api.DepthFinding
import modulecheck.model.sourceset.SourceSetName
import modulecheck.reporting.logging.Report

class DepthLogFactory {

  fun create(results: List<DepthFinding>): Report = Report.build {

    headerLine("-- ModuleCheck main source set depth results --")

    val depthHeader = "depth"
    val childrenHeader = "modules"

    val depthHeaderLength = depthHeader.length + DEPTH_PADDING

    headerLine(
      "    " +
        "${depthHeader.padEnd(depthHeaderLength)} " +
        childrenHeader
    )

    results
      .filter { it.sourceSetName == SourceSetName.MAIN }
      .groupBy { it.depth }
      .toSortedMap()
      .entries
      .forEach { (depth, values) ->

        val paths = values.distinctBy { it.dependentPath }
          .sortedBy { it.dependentPath }
          .joinToString(
            separator = ", ",
            prefix = "[",
            postfix = "]"
          ) { it.dependentPath.value }

        infoLine("    ${depth.toString().padEnd(depthHeaderLength)} $paths")
      }

    // bottom padding
    headerLine("")
  }

  companion object {

    private const val DEPTH_PADDING = 3
  }
}
