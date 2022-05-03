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

import modulecheck.reporting.logging.Report
import modulecheck.rule.finding.Finding
import java.util.Locale
import javax.inject.Inject

class ReportFactory @Inject constructor() {

  fun create(results: List<Finding.FindingResult>): Report = Report.build {

    header("-- ModuleCheck results --")

    results.groupBy { it.dependentPath.value.lowercase(Locale.getDefault()) }
      .entries
      .sortedBy { it.key }
      .forEach { (_, values) ->

        val path = values.first().dependentPath

        header("\n${tab(1)}${path.value}")

        val maxConfigurationName = maxOf(
          values.maxOf { it.configurationName.length },
          "configuration".length
        )
        val maxDependencyPath = maxOf(
          values.maxOf { it.dependencyIdentifier.length },
          "dependency".length
        )
        val maxProblemName = values.maxOf { it.ruleName.id.length }
        val maxSource = maxOf(values.maxOf { it.sourceOrNull.orEmpty().length }, "source".length)

        val fixPrefix = "   ​"

        header(
          tab(2) +
            fixPrefix +
            "configuration".padEnd(maxConfigurationName) +
            tab(1) +
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
            { it.dependencyIdentifier },
            { it.positionOrNull },
            { it.ruleName.id },
            { it.sourceOrNull }
          )
        ).forEach { result ->

          val message = result.configurationName.padEnd(maxConfigurationName) +
            tab(1) +
            result.dependencyIdentifier.padEnd(maxDependencyPath) +
            tab(1) +
            result.ruleName.id.padEnd(maxProblemName) +
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

  // use `​` (\u200B) as an invisible token for parsing.
  private fun tab(numTabs: Int) = "\u200B    ".repeat(numTabs)
}
