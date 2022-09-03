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

package modulecheck.reporting.logging

import modulecheck.reporting.logging.Report.ReportEntry.AppendNewLine
import modulecheck.reporting.logging.Report.ReportEntry.Failure
import modulecheck.reporting.logging.Report.ReportEntry.FailureHeader
import modulecheck.reporting.logging.Report.ReportEntry.FailureLine
import modulecheck.reporting.logging.Report.ReportEntry.Header
import modulecheck.reporting.logging.Report.ReportEntry.HeaderLine
import modulecheck.reporting.logging.Report.ReportEntry.Info
import modulecheck.reporting.logging.Report.ReportEntry.InfoLine
import modulecheck.reporting.logging.Report.ReportEntry.Success
import modulecheck.reporting.logging.Report.ReportEntry.SuccessHeader
import modulecheck.reporting.logging.Report.ReportEntry.SuccessLine
import modulecheck.reporting.logging.Report.ReportEntry.Warning
import modulecheck.reporting.logging.Report.ReportEntry.WarningLine

interface McLogger {
  /** @since 0.11.0 */
  fun printReport(report: Report)

  /** @since 0.11.0 */
  fun printHeader(message: String)

  /** @since 0.11.0 */
  fun printHeaderLine(message: String)

  /** @since 0.11.0 */
  fun printWarning(message: String)

  /** @since 0.11.0 */
  fun printWarningLine(message: String)

  /** @since 0.11.0 */
  fun printInfo(message: String)

  /** @since 0.11.0 */
  fun printInfoLine(message: String)

  /** @since 0.11.0 */
  fun printFailure(message: String)

  /** @since 0.11.0 */
  fun printFailureLine(message: String)

  /** @since 0.11.0 */
  fun printFailureHeader(message: String)

  /** @since 0.11.0 */
  fun printSuccess(message: String)

  /** @since 0.11.0 */
  fun printSuccessLine(message: String)

  /** @since 0.11.0 */
  fun printSuccessHeader(message: String)
}

data class Report(val entries: List<ReportEntry>) {

  fun joinToString(): String = buildString {
    entries
      .forEach { reportEntry ->
        when (reportEntry) {
          is AppendNewLine -> appendLine(reportEntry.message.trimEnd())
          else -> append(reportEntry.message)
        }
      }
  }

  sealed interface ReportEntry {
    val message: String

    /** @since 0.11.0 */
    @JvmInline
    value class Header(override val message: String) : ReportEntry

    /** @since 0.11.0 */
    @JvmInline
    value class HeaderLine(override val message: String) : ReportEntry, AppendNewLine

    /** @since 0.11.0 */
    @JvmInline
    value class Warning(override val message: String) : ReportEntry

    /** @since 0.11.0 */
    @JvmInline
    value class WarningLine(override val message: String) : ReportEntry, AppendNewLine

    /** @since 0.11.0 */
    @JvmInline
    value class Info(override val message: String) : ReportEntry

    /** @since 0.11.0 */
    @JvmInline
    value class InfoLine(override val message: String) : ReportEntry, AppendNewLine

    /** @since 0.11.0 */
    @JvmInline
    value class Failure(override val message: String) : ReportEntry

    /** @since 0.11.0 */
    @JvmInline
    value class FailureLine(override val message: String) : ReportEntry, AppendNewLine

    /** @since 0.11.0 */
    @JvmInline
    value class FailureHeader(override val message: String) : ReportEntry, AppendNewLine

    /** @since 0.11.0 */
    @JvmInline
    value class Success(override val message: String) : ReportEntry

    /** @since 0.11.0 */
    @JvmInline
    value class SuccessLine(override val message: String) : ReportEntry, AppendNewLine

    /** @since 0.11.0 */
    @JvmInline
    value class SuccessHeader(override val message: String) : ReportEntry, AppendNewLine

    interface AppendNewLine

    fun printToStdOut() {
      when (this) {
        is AppendNewLine -> println(message.trimEnd())
        else -> print(message)
      }
    }
  }

  class ReportBuilder(
    private val entries: MutableList<ReportEntry> = mutableListOf()
  ) {

    /** @since 0.11.0 */
    fun header(message: String) {
      entries.add(Header(message.trimEnd()))
    }

    /** @since 0.11.0 */
    fun headerLine(message: String) {
      entries.add(HeaderLine(message.trimEnd()))
    }

    /** @since 0.11.0 */
    fun warning(message: String) {
      entries.add(Warning(message))
    }

    /** @since 0.11.0 */
    fun warningLine(message: String) {
      entries.add(WarningLine(message.trimEnd()))
    }

    /** @since 0.11.0 */
    fun info(message: String) {
      entries.add(Info(message))
    }

    /** @since 0.11.0 */
    fun infoLine(message: String) {
      entries.add(InfoLine(message))
    }

    /** @since 0.11.0 */
    fun failure(message: String) {
      entries.add(Failure(message))
    }

    /** @since 0.11.0 */
    fun failureLine(message: String) {
      entries.add(FailureLine(message.trimEnd()))
    }

    /** @since 0.11.0 */
    fun failureHeader(message: String) {
      entries.add(FailureHeader(message.trimEnd()))
    }

    /** @since 0.11.0 */
    fun success(message: String) {
      entries.add(Success(message))
    }

    /** @since 0.11.0 */
    fun successLine(message: String) {
      entries.add(SuccessLine(message.trimEnd()))
    }

    /** @since 0.11.0 */
    fun successHeader(message: String) {
      entries.add(SuccessHeader(message.trimEnd()))
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
