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

package modulecheck.api.test

import modulecheck.api.Logger
import modulecheck.api.Report
import modulecheck.api.Report.ReportEntry
import modulecheck.api.Report.ReportEntry.*

class ReportingLogger(
  private val mirrorToStandardOut: Boolean = true
) : Logger {

  private val entries = mutableListOf<ReportEntry>()

  fun collectReport(): Report {
    return Report(entries)
  }

  fun clear() {
    entries.clear()
  }

  private fun addEntry(reportEntry: ReportEntry) {
    entries.add(reportEntry)
    if (mirrorToStandardOut) {
      reportEntry.printToStdOut()
    }
  }

  override fun printReport(report: Report) {
    entries.addAll(report.entries)
    if (mirrorToStandardOut) {
      report.entries
        .forEach { it.printToStdOut() }
      println()
    }
  }

  override fun printHeader(message: String) {
    addEntry(Header(message))
  }

  override fun printWarning(message: String) {
    addEntry(Warning(message))
  }

  override fun printWarningLine(message: String) {
    addEntry(WarningLine(message))
  }

  override fun printInfo(message: String) {
    addEntry(Info(message))
  }

  override fun printFailure(message: String) {
    addEntry(Failure(message))
  }

  override fun printFailureLine(message: String) {
    addEntry(FailureLine(message))
  }

  override fun printFailureHeader(message: String) {
    addEntry(FailureHeader(message))
  }

  override fun printSuccess(message: String) {
    addEntry(Success(message))
  }

  override fun printSuccessLine(message: String) {
    addEntry(SuccessLine(message))
  }

  override fun printSuccessHeader(message: String) {
    addEntry(SuccessHeader(message))
  }
}
