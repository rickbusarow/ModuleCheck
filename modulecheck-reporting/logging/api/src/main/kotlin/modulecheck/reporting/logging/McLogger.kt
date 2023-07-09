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

package modulecheck.reporting.logging

import modulecheck.reporting.logging.Report.ReportEntry.Failure
import modulecheck.reporting.logging.Report.ReportEntry.Info
import modulecheck.reporting.logging.Report.ReportEntry.Success
import modulecheck.reporting.logging.Report.ReportEntry.Warning

interface McLogger {
  /** @since 0.11.0 */
  fun printReport(report: Report)

  /** @since 0.11.0 */
  fun warning(message: String)

  /** @since 0.11.0 */
  fun info(message: String)

  /** @since 0.11.0 */
  fun failure(message: String)

  /** @since 0.11.0 */
  fun success(message: String)
}

data class Report(val entries: List<ReportEntry>) {

  fun joinToString(): String = buildString {
    entries.forEach { reportEntry ->
      when (reportEntry) {
        is Failure -> appendLine(reportEntry.message)
        is Info -> appendLine(reportEntry.message)
        is Success -> appendLine(reportEntry.message)
        is Warning -> appendLine(reportEntry.message)
      }
    }
  }

  sealed interface ReportEntry {
    val message: String

    /** @since 0.11.0 */
    @JvmInline
    value class Warning(override val message: String) : ReportEntry

    /** @since 0.11.0 */
    @JvmInline
    value class Info(override val message: String) : ReportEntry

    /** @since 0.11.0 */
    @JvmInline
    value class Failure(override val message: String) : ReportEntry

    /** @since 0.11.0 */
    @JvmInline
    value class Success(override val message: String) : ReportEntry

    fun printToStdOut() {
      when (this) {
        is Failure -> println(message)
        is Info -> println(message)
        is Success -> println(message)
        is Warning -> println(message)
      }
    }
  }

  class ReportBuilder(
    private val entries: MutableList<ReportEntry> = mutableListOf()
  ) {

    /** @since 0.11.0 */
    fun warning(message: String) {
      entries.add(Warning(message))
    }

    /** @since 0.11.0 */
    fun info(message: String) {
      entries.add(Info(message))
    }

    /** @since 0.11.0 */
    fun failure(message: String) {
      entries.add(Failure(message))
    }

    /** @since 0.11.0 */
    fun success(message: String) {
      entries.add(Success(message))
    }
  }

  companion object {

    fun build(buildAction: ReportBuilder.() -> Unit): Report {
      val entries = mutableListOf<ReportEntry>()

      ReportBuilder(entries).buildAction()

      return Report(entries)
    }
  }
}
