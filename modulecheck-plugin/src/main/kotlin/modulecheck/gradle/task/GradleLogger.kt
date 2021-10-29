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

package modulecheck.gradle.task

import modulecheck.api.Logger
import modulecheck.api.Report
import modulecheck.api.Report.ReportEntry.*
import modulecheck.gradle.GradleProject
import org.gradle.configurationcache.extensions.serviceOf
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory

class GradleLogger(project: GradleProject) : Logger {

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
    output.withStyle(StyledTextOutput.Style.Info).println(message)
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
