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

package modulecheck.api

import modulecheck.api.Report.ReportEntry.*

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

    @JvmInline
    value class Header(override val message: String) : ReportEntry, AppendNewLine

    @JvmInline
    value class Warning(override val message: String) : ReportEntry

    @JvmInline
    value class WarningLine(override val message: String) : ReportEntry, AppendNewLine

    @JvmInline
    value class Info(override val message: String) : ReportEntry, AppendNewLine

    @JvmInline
    value class Failure(override val message: String) : ReportEntry

    @JvmInline
    value class FailureLine(override val message: String) : ReportEntry, AppendNewLine

    @JvmInline
    value class FailureHeader(override val message: String) : ReportEntry, AppendNewLine

    @JvmInline
    value class Success(override val message: String) : ReportEntry

    @JvmInline
    value class SuccessLine(override val message: String) : ReportEntry, AppendNewLine

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

  class ReportBuilderScope(
    private val entries: MutableList<ReportEntry> = mutableListOf()
  ) {

    fun header(message: String) {
      entries.add(Header(message.trimEnd()))
    }

    fun warning(message: String) {
      entries.add(Warning(message))
    }

    fun warningLine(message: String) {
      entries.add(WarningLine(message.trimEnd()))
    }

    fun info(message: String) {
      entries.add(Info(message))
    }

    fun failure(message: String) {
      entries.add(Failure(message))
    }

    fun failureLine(message: String) {
      entries.add(FailureLine(message.trimEnd()))
    }

    fun failureHeader(message: String) {
      entries.add(FailureHeader(message.trimEnd()))
    }

    fun success(message: String) {
      entries.add(Success(message))
    }

    fun successLine(message: String) {
      entries.add(SuccessLine(message.trimEnd()))
    }

    fun successHeader(message: String) {
      entries.add(SuccessHeader(message.trimEnd()))
    }
  }

  companion object {

    fun build(buildAction: ReportBuilderScope.() -> Unit): Report {
      val entries = mutableListOf<ReportEntry>()

      ReportBuilderScope(entries).buildAction()

      return Report(entries)
    }
  }
}
