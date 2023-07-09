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

import modulecheck.api.DepthFinding

/**
 * Creates the depth report content as it is written to a text file -- not the console.
 *
 * ex:
 * ```
 * -- ModuleCheck Depth results --
 *
 * :app
 *     source set      depth    most expensive dependencies
 *     main            2        [:lib2]
 *
 * :lib2
 *     source set      depth    most expensive dependencies
 *     main            1        [:lib1]
 * ```
 */
class DepthReportFactory {
  /** */
  fun create(results: List<DepthFinding>): String = buildString {

    appendLine("-- ModuleCheck Depth results --")

    results.filter { it.shouldReport() }
      .groupBy { it.dependentPath }
      .toSortedMap()
      .entries
      .forEach { (path, values) ->

        appendLine("\n${path.value}")

        val nameHeader = "source set"
        val depthHeader = "depth"
        val childrenHeader = "most expensive dependencies"

        val maxSourceSetNameLength = values.map { it.sourceSetName.value }
          .plus(nameHeader)
          .maxOf { it.length } + SOURCE_SET_NAME_PADDING

        val depthHeaderLength = depthHeader.length + DEPTH_PADDING

        appendLine(
          "    " +
            "${nameHeader.padEnd(maxSourceSetNameLength)} " +
            "${depthHeader.padEnd(depthHeaderLength)} " +
            childrenHeader
        )

        values.sortedBy { it.sourceSetName.value }
          .forEach { depthFinding ->
            appendLine(
              "    " +
                "${depthFinding.sourceSetName.value.padEnd(maxSourceSetNameLength)} " +
                "${depthFinding.depth.toString().padEnd(depthHeaderLength)} " +
                depthFinding.children.joinToString(
                  separator = ", ",
                  prefix = "[",
                  postfix = "]"
                ) { it.dependentPath.value }
            )
          }
      }

    // bottom padding
    appendLine("")
  }

  private fun DepthFinding.shouldReport(): Boolean {
    // If the module declares dependencies, report it even if it has no source set files.
    if (depth > 0) return true

    // This really shouldn't be possible, but just skip reporting if the source set doesn't exist
    val sourceSet = dependentProject.sourceSets[sourceSetName] ?: return false

    // If the depth is 0 and there are no files, just omit the report.
    // If the depth is 0 and it has files, like a core module, report it.
    return sourceSet.hasExistingSourceFiles
  }

  companion object {

    private const val SOURCE_SET_NAME_PADDING = 5
    private const val DEPTH_PADDING = 3
  }
}
