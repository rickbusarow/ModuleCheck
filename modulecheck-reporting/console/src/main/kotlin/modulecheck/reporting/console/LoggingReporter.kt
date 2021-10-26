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

package modulecheck.reporting.console

import modulecheck.api.Finding
import modulecheck.api.Logger
import java.util.*

class LoggingReporter(
  private val logger: Logger
) {

  fun reportResults(results: List<Finding.FindingResult>) {

    if (results.isEmpty()) return

    logger.printHeader("-- ModuleCheck results --")

    results.groupBy { it.dependentPath.lowercase(Locale.getDefault()) }
      .entries
      .forEach { (_, values) ->

        val path = values.first().dependentPath

        logger.printHeader("\n${tab(1)}$path")

        val maxDependencyPath = maxOf(
          values.maxOf { it.dependencyPath.length },
          "dependency".length
        )
        val maxProblemName = values.maxOf { it.problemName.length }
        val maxSource = maxOf(values.maxOf { it.sourceOrNull.orEmpty().length }, "source".length)
        val maxFilePathStr = values.maxOf { it.filePathString.length }

        logger.printHeader(
          tab(2) +
            "   dependency".padEnd(maxDependencyPath) +
            tab(1) +
            "name".padEnd(maxProblemName) +
            tab(1) +
            "source".padEnd(maxSource) +
            tab(1) +
            "build file".padEnd(maxFilePathStr)
        )

        values.sortedWith(
          compareBy(
            { !it.fixed },
            { it.positionOrNull }
          )
        ).forEach { result ->

          val message = result.dependencyPath.padEnd(maxDependencyPath) +
            tab(1) +
            result.problemName.padEnd(maxProblemName) +
            tab(1) +
            result.sourceOrNull.orEmpty().padEnd(maxSource) +
            tab(1) +
            result.filePathString.padEnd(maxFilePathStr)

          if (result.fixed) {
            logger.printSuccess(tab(2) + "✔  ")
            logger.printWarningLine(message)
          } else {
            logger.printFailure(tab(2) + "❌  ")
            logger.printFailureLine(message)
          }
        }
      }

    // bottom padding
    logger.printHeader("")
  }

  private fun tab(numTabs: Int) = "    ".repeat(numTabs)
}
