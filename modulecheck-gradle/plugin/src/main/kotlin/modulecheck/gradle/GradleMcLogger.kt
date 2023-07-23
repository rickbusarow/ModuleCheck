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

package modulecheck.gradle

import com.github.ajalt.mordant.rendering.Theme
import com.github.ajalt.mordant.terminal.Terminal
import com.squareup.anvil.annotations.ContributesBinding
import modulecheck.dagger.DaggerLazy
import modulecheck.dagger.TaskScope
import modulecheck.gradle.platforms.internal.GradleLogging
import modulecheck.reporting.logging.McLogger
import modulecheck.reporting.logging.Report
import modulecheck.reporting.logging.Report.ReportEntry.Failure
import modulecheck.reporting.logging.Report.ReportEntry.Info
import modulecheck.reporting.logging.Report.ReportEntry.Success
import modulecheck.reporting.logging.Report.ReportEntry.Warning
import org.gradle.api.logging.Logger
import javax.inject.Inject

@ContributesBinding(TaskScope::class)
class GradleMcLogger @Inject constructor(
  private val terminalLazy: DaggerLazy<Terminal>
) : McLogger {

  private val logger: Logger by lazy { GradleLogging.getLogger("ktlint logger Gradle") }

  private val theme: Theme by lazy { terminalLazy.get().theme }

  override fun printReport(report: Report) {
    report.entries
      .forEach { reportEntry ->
        when (reportEntry) {
          is Failure -> failure(reportEntry.message)
          is Info -> info(reportEntry.message)
          is Success -> success(reportEntry.message)
          is Warning -> warning(reportEntry.message)
        }
      }
  }

  override fun warning(message: String) {
    logger.lifecycle(theme.warning(message))
  }

  override fun info(message: String) {
    logger.lifecycle(theme.info(message))
  }

  override fun failure(message: String) {
    logger.lifecycle(theme.danger(message))
  }

  override fun success(message: String) {
    logger.lifecycle(theme.success(message))
  }
}
