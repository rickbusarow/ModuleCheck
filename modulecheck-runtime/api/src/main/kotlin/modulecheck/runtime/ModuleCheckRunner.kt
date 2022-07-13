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

package modulecheck.runtime

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dispatch.core.DispatcherProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import modulecheck.api.DepthFinding
import modulecheck.api.context.ProjectDepth
import modulecheck.config.ModuleCheckSettings
import modulecheck.dagger.DaggerList
import modulecheck.finding.Finding
import modulecheck.finding.Finding.FindingResult
import modulecheck.finding.FindingResultFactory
import modulecheck.finding.Problem
import modulecheck.project.McProject
import modulecheck.project.ProjectProvider
import modulecheck.reporting.checkstyle.CheckstyleReporter
import modulecheck.reporting.console.DepthLogFactory
import modulecheck.reporting.console.DepthReportFactory
import modulecheck.reporting.console.ReportFactory
import modulecheck.reporting.graphviz.GraphvizFileWriter
import modulecheck.reporting.logging.McLogger
import modulecheck.reporting.sarif.SarifReportFactory
import modulecheck.rule.FindingFactory
import modulecheck.rule.ModuleCheckRule
import modulecheck.utils.coroutines.mapAsync
import modulecheck.utils.createSafely
import modulecheck.utils.letIf
import modulecheck.utils.trace.Trace
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.system.measureTimeMillis

/**
 * Proxy for a Gradle task, without all the Gradle framework stuff. Most logic is delegated to its
 * various dependencies.
 *
 * @param findingFactory handles parsing of the projects in order to generate the findings
 * @param findingResultFactory attempts to apply fixes to the findings and returns a list of
 *   [FindingResult][modulecheck.finding.Finding.FindingResult]
 * @param reportFactory handles console output of the results
 */
@Suppress("LongParameterList")
data class ModuleCheckRunner @AssistedInject constructor(
  val settings: ModuleCheckSettings,
  val findingFactory: FindingFactory<out Finding>,
  val logger: McLogger,
  val findingResultFactory: FindingResultFactory,
  val reportFactory: ReportFactory,
  val checkstyleReporter: CheckstyleReporter,
  val graphvizFileWriter: GraphvizFileWriter,
  val dispatcherProvider: DispatcherProvider,
  val sarifReportFactory: SarifReportFactory,
  val projectProvider: ProjectProvider,
  val rules: DaggerList<ModuleCheckRule<*>>,
  @Assisted
  val autoCorrect: Boolean
) {

  fun run(projects: List<McProject>): Result<Unit> = runBlocking(
    dispatcherProvider.default
      .letIf<CoroutineContext>(settings.trace) {
        it + Trace.start(ModuleCheckRunner::class)
      }
  ) {
    // total findings, whether they're fixed or not
    var totalFindings = 0

    val allFindings = mutableListOf<Finding>()

    // TODO - delete me
    projects.flatMap { it.sourceSets.values }
      .mapAsync { it.kotlinEnvironmentDeferred.await().bindingContextDeferred.await() }
      .toList()

    // number of findings which couldn't be fixed
    // time does not include initial parsing from GradleProjectProvider,
    // but does include all source file parsing and the amount of time spent applying fixes
    val resultsWithTime = measured {
      val fixableFindings = findingFactory.evaluateFixable(projects).distinct()

      val fixableResults = fixableFindings.filterIsInstance<Problem>()
        .filterNot { it.isSuppressed.await() }
        .also { totalFindings += it.size }
        .let { processFindings(it) }

      projectProvider.clearCaches()

      val sortFindings = findingFactory.evaluateSorts(projectProvider.getAll())
        .distinct()

      val sortsResults = sortFindings.filterIsInstance<Problem>()
        .filterNot { it.isSuppressed.await() }
        .also { totalFindings += it.size }
        .let { processFindings(it) }

      projectProvider.clearCaches()

      val reportOnlyFindings = findingFactory.evaluateReports(projectProvider.getAll())
        .distinct()

      processFindings(reportOnlyFindings)

      allFindings += (fixableFindings + sortFindings + reportOnlyFindings)

      fixableResults + sortsResults // + reportResults
    }

    val allResults = resultsWithTime.data

    reportResults(allResults)

    val totalUnfixedIssues = allResults.count { !it.fixed }

    val depths = allFindings.filterIsInstance<DepthFinding>()
    maybeLogDepths(depths)
    maybeReportDepths(depths)
    maybeCreateGraphs(depths.map { it.toProjectDepth() })

    // Replace this with kotlinx Duration APIs as soon as it's stable
    @Suppress("MagicNumber")
    val secondsDouble = resultsWithTime.timeMillis / 1000.0

    val issuePlural = if (totalFindings == 1) "issue" else "issues"

    logger.printInfo("ModuleCheck found $totalFindings $issuePlural in $secondsDouble seconds.")

    if (totalFindings > 0) {

      logger.printWarning(
        "\n\nTo ignore any of these findings, annotate the dependency declaration with " +
          "@Suppress(\"<the name of the issue>\") in Kotlin, or " +
          "//noinspection <the name of the issue> in Groovy.\n" +
          "See https://rbusarow.github.io/ModuleCheck/docs/suppressing-findings for more info."
      )
    }

    if (totalUnfixedIssues > 0) {

      val wasPlural = if (totalFindings == 1) "was" else "were"

      Result.failure(
        ModuleCheckFailure(
          "ModuleCheck found $totalUnfixedIssues $issuePlural which $wasPlural not auto-corrected."
        )
      )
    } else {
      Result.success(Unit)
    }
  }

  /** Tries to fix all findings one project at a time, then reports the results. */
  private suspend fun processFindings(findings: List<Finding>): List<FindingResult> {
    return findingResultFactory.create(
      findings = findings,
      autoCorrect = autoCorrect,
      deleteUnused = settings.deleteUnused
    )
  }

  /** Creates any applicable reports. */
  private fun reportResults(results: List<Finding.FindingResult>) {

    val textReport = reportFactory.create(results)

    if (results.isNotEmpty()) {
      logger.printReport(textReport)
    }

    if (settings.reports.text.enabled) {
      val path = settings.reports.text.outputPath

      File(path)
        .createSafely(textReport.joinToString())
    }

    if (settings.reports.sarif.enabled) {
      val path = settings.reports.sarif.outputPath

      val sarif = sarifReportFactory.create(results, rules)

      File(path)
        .createSafely(sarif)
    }

    if (settings.reports.checkstyle.enabled) {
      val path = settings.reports.checkstyle.outputPath

      File(path)
        .createSafely(checkstyleReporter.createXml(results))
    }
  }

  private fun maybeLogDepths(depths: List<DepthFinding>) {

    if (depths.isEmpty()) return

    // TODO - this should probably be replaced with an explicit runtime argument, or the depths
    //  report should be folded into something like a "module metrics" finding/rule, so that
    //  `DepthFinding` can just be treated like a primitive.
    // If the depths check is enabled, then just always log them.  If the check is disabled but
    // this is a single-rule "task", that means the user is explicitly running 'moduleCheckDepths'
    // (or whatever that has been renamed to in the future) and the results should always be logged.
    if (settings.checks.depths || rules.size == 1) {
      val depthLog = DepthLogFactory().create(depths)

      logger.printReport(depthLog)
    }
  }

  private fun maybeReportDepths(depths: List<DepthFinding>) {
    if (settings.reports.depths.enabled) {
      val path = settings.reports.depths.outputPath

      val depthReport = DepthReportFactory().create(depths)

      File(path)
        .createSafely(depthReport.joinToString())
    }
  }

  private suspend fun maybeCreateGraphs(depths: List<ProjectDepth>) {
    if (settings.reports.graphs.enabled) {
      graphvizFileWriter.write(depths)
    }
  }

  private inline fun <T, R> T.measured(action: T.() -> R): TimedResults<R> {
    var r: R

    val time = measureTimeMillis {
      r = action()
    }

    return TimedResults(time, r)
  }

  data class TimedResults<R>(val timeMillis: Long, val data: R)

  @AssistedFactory
  interface Factory {
    fun create(autoCorrect: Boolean): ModuleCheckRunner
  }
}

private class ModuleCheckFailure(message: String) : Exception(message)
