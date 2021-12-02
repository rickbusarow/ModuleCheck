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
import modulecheck.project.Report
import java.util.Locale
import javax.inject.Inject

class ReportFactory @Inject constructor() {

  fun create(results: List<Finding.FindingResult>): Report = Report.build {

    header("-- ModuleCheck results --")

    results.groupBy { it.dependentPath.lowercase(Locale.getDefault()) }
      .entries
      .forEach { (_, values) ->

        val path = values.first().dependentPath

        header("\n${tab(1)}$path")

        val maxDependencyPath = maxOf(
          values.maxOf { it.dependencyPath.length },
          "dependency".length
        )
        val maxProblemName = values.maxOf { it.problemName.length }
        val maxSource = maxOf(values.maxOf { it.sourceOrNull.orEmpty().length }, "source".length)

        val fixPrefix = "   "

        header(
          tab(2) +
            fixPrefix +
            "dependency".padEnd(maxDependencyPath) +
            tab(1) +
            "name".padEnd(maxProblemName) +
            tab(1) +
            "source".padEnd(maxSource) +
            tab(1) +
            "build file"
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
            result.filePathString

          if (result.fixed) {
            success(tab(2) + fixPrefix.replaceFirst(" ", "✔"))
            warningLine(message)
          } else {
            failure(tab(2) + fixPrefix.replaceFirst(" ", "X"))
            failureLine(message)
          }
        }
      }

    // bottom padding
    header("")
  }

  private fun tab(numTabs: Int) = "    ".repeat(numTabs)
}
