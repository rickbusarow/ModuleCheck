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

package modulecheck.reporting.logging.test

import modulecheck.reporting.logging.McLogger
import modulecheck.reporting.logging.Report
import modulecheck.reporting.logging.Report.ReportEntry
import modulecheck.reporting.logging.Report.ReportEntry.Failure
import modulecheck.reporting.logging.Report.ReportEntry.Info
import modulecheck.reporting.logging.Report.ReportEntry.Success
import modulecheck.reporting.logging.Report.ReportEntry.Warning

class ReportingLogger(
  private val mirrorToStandardOut: Boolean = true
) : McLogger {

  private val entries = mutableListOf<ReportEntry>()

  fun collectReport(): Report = Report(entries)

  fun clear() {
    entries.clear()
  }

  private fun addEntry(reportEntry: ReportEntry) {
    entries.add(reportEntry)
    if (mirrorToStandardOut) {
      reportEntry.printToStdOut()
      println()
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

  override fun failure(message: String) = addEntry(Failure(message))
  override fun info(message: String) = addEntry(Info(message))
  override fun success(message: String) = addEntry(Success(message))
  override fun warning(message: String) = addEntry(Warning(message))
}
