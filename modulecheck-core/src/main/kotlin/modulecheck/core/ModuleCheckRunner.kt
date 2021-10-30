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

package modulecheck.core

import modulecheck.api.*
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.parsing.McProject
import modulecheck.reporting.checkstyle.CheckstyleReporter
import modulecheck.reporting.console.ReportFactory
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * Proxy for a Gradle task, without all the Gradle framework stuff. Most logic is delegated to its
 * various dependencies.
 *
 * @param findingFactory handles parsing of the projects in order to generate the findings
 * @param findingResultFactory attempts to apply fixes to the findings and returns a list of
 *   [FindingResult][modulecheck.api.Finding.FindingResult]
 * @param reportFactory handles console output of the results
 */
class ModuleCheckRunner(
  val settings: ModuleCheckSettings,
  val findingFactory: FindingFactory<out Finding>,
  val logger: Logger,
  val findingResultFactory: FindingResultFactory = RealFindingResultFactory(),
  val reportFactory: ReportFactory = ReportFactory(),
  val checkstyleReporter: CheckstyleReporter = CheckstyleReporter()
) {

  fun run(projects: List<McProject>): Result<Unit> {

    // total findings, whether they're fixed or not
    var totalFindings = 0

    // number of findings which couldn't be fixed
    // time does not include initial parsing from GradleProjectProvider,
    // but does include all source file parsing and the amount of time spent applying fixes
    val unfixedCountWithTime = measured {
      findingFactory.evaluate(projects)
        .distinct()
        .filterNot { it.shouldSkip() }
        .also { totalFindings = it.size }
        .let { processFindings(it) }
    }

    val totalUnfixedIssues = unfixedCountWithTime.data

    // Replace this with kotlinx Duration APIs as soon as it's stable
    @Suppress("MagicNumber")
    val secondsDouble = unfixedCountWithTime.timeMillis / 1000.0

    if (totalFindings > 0) {
      logger.printInfo(
        "ModuleCheck found $totalFindings issues in $secondsDouble seconds.\n\n" +
          "To ignore any of these findings, annotate the dependency declaration with " +
          "@Suppress(\"<the name of the issue>\") in Kotlin, or " +
          "//noinspection <the name of the issue> in Groovy.\n" +
          "See https://rbusarow.github.io/ModuleCheck/docs/suppressing-findings for more info."
      )
    }

    return if (totalUnfixedIssues > 0) {
      Result.failure(
        ModuleCheckFailure(
          "ModuleCheck found $totalUnfixedIssues issues which were not auto-corrected."
        )
      )
    } else {
      Result.success(Unit)
    }
  }

  /**
   * Tries to fix all findings one project at a time, then reports the results.
   */
  private fun processFindings(findings: List<Finding>): Int {

    // TODO - The order of applying fixes is stable, which may be important in troubleshooting, but
    //   it's probably not perfect. There is a chance that up-stream changes to a dependency can
    //   change what must be done to the dependent module. This *should* be mitigated by analyzing
    //   everything before applying any fixes, and by including "inherited" and "overshot"
    //   dependencies.
    //
    //   If stability ever becomes a problem, the next step would be to try applying
    //   fixes by order of "depth", where the highest-depth modules are changed first.  This would
    //   have to be done **per source set**, where changes are only applied to that source set's
    //   configs.  The main source set would be done first for the entire project tree, then test,
    //   debug, etc.
    val results = findings
      .groupBy { it.dependentPath }
      .flatMap { (_, list) ->

        findingResultFactory.create(
          findings = list,
          autoCorrect = settings.autoCorrect,
          deleteUnused = settings.deleteUnused
        )
      }

    val textReport = reportFactory.create(results)

    if (results.isNotEmpty()) {
      logger.printReport(textReport)
    }

    if (settings.reports.text.enabled) {
      val path = settings.reports.text.outputPath

      File(path)
        .also { it.parentFile.mkdirs() }
        .writeText(textReport.joinToString())
    }

    if (settings.reports.checkstyle.enabled) {
      val path = settings.reports.checkstyle.outputPath

      File(path)
        .also { it.parentFile.mkdirs() }
        .writeText(checkstyleReporter.createXml(results))
    }

    return results.count { !it.fixed }
  }

  private inline fun <T, R> T.measured(action: T.() -> R): TimedResults<R> {
    var r: R

    val time = measureTimeMillis {
      r = action()
    }

    return TimedResults(time, r)
  }

  data class TimedResults<R>(val timeMillis: Long, val data: R)
}

private class ModuleCheckFailure(message: String) : Exception(message)
