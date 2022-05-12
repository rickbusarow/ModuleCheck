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

package modulecheck.gradle

import com.squareup.anvil.annotations.ContributesBinding
import modulecheck.dagger.AppScope
import modulecheck.dagger.RootGradleProject
import modulecheck.reporting.logging.McLogger
import modulecheck.reporting.logging.Report
import modulecheck.reporting.logging.Report.ReportEntry.Failure
import modulecheck.reporting.logging.Report.ReportEntry.FailureHeader
import modulecheck.reporting.logging.Report.ReportEntry.FailureLine
import modulecheck.reporting.logging.Report.ReportEntry.Header
import modulecheck.reporting.logging.Report.ReportEntry.Info
import modulecheck.reporting.logging.Report.ReportEntry.Success
import modulecheck.reporting.logging.Report.ReportEntry.SuccessHeader
import modulecheck.reporting.logging.Report.ReportEntry.SuccessLine
import modulecheck.reporting.logging.Report.ReportEntry.Warning
import modulecheck.reporting.logging.Report.ReportEntry.WarningLine
import org.gradle.configurationcache.extensions.serviceOf
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import javax.inject.Inject
import org.gradle.api.Project as GradleProject

@ContributesBinding(AppScope::class)
class GradleMcLogger @Inject constructor(
  @RootGradleProject
  project: GradleProject
) : McLogger {

  private val output: StyledTextOutput = project
    .serviceOf<StyledTextOutputFactory>()
    .create("modulecheck")

  override fun printReport(report: Report) {
    report.entries
      .forEach { reportEntry ->
        when (reportEntry) {
          is Failure -> printFailure(reportEntry.message)
          is FailureHeader -> printFailureHeader(reportEntry.message)
          is FailureLine -> printFailureLine(reportEntry.message)
          is Header -> printHeader(reportEntry.message)
          is Info -> printInfo(reportEntry.message)
          is Success -> printSuccess(reportEntry.message)
          is SuccessHeader -> printSuccessHeader(reportEntry.message)
          is SuccessLine -> printSuccessLine(reportEntry.message)
          is Warning -> printWarning(reportEntry.message)
          is WarningLine -> printWarningLine(reportEntry.message)
        }
      }
  }

  override fun printHeader(message: String) {
    output.withStyle(StyledTextOutput.Style.Header).println(message)
  }

  override fun printWarning(message: String) {
    output.withStyle(StyledTextOutput.Style.Description).text(message)
  }

  override fun printWarningLine(message: String) {
    output.withStyle(StyledTextOutput.Style.Description).println(message)
  }

  override fun printInfo(message: String) {
    output.withStyle(StyledTextOutput.Style.Description).println(message)
  }

  override fun printFailure(message: String) {
    output.withStyle(StyledTextOutput.Style.Failure).text(message)
  }

  override fun printFailureLine(message: String) {
    output.withStyle(StyledTextOutput.Style.Failure).println(message)
  }

  override fun printFailureHeader(message: String) {
    output.withStyle(StyledTextOutput.Style.FailureHeader).println(message)
  }

  override fun printSuccess(message: String) {
    output.withStyle(StyledTextOutput.Style.Success).text(message)
  }

  override fun printSuccessLine(message: String) {
    output.withStyle(StyledTextOutput.Style.Success).println(message)
  }

  override fun printSuccessHeader(message: String) {
    output.withStyle(StyledTextOutput.Style.SuccessHeader).println(message)
  }
}
