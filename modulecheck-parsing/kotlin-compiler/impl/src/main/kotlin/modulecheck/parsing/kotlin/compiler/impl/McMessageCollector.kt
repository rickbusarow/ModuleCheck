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

package modulecheck.parsing.kotlin.compiler.impl

import modulecheck.parsing.kotlin.compiler.impl.McMessageCollector.LogLevel.ERRORS
import modulecheck.parsing.kotlin.compiler.impl.McMessageCollector.LogLevel.VERBOSE
import modulecheck.parsing.kotlin.compiler.impl.McMessageCollector.LogLevel.WARNINGS_AS_ERRORS
import modulecheck.parsing.kotlin.compiler.impl.McMessageCollector.LogLevel.WARNINGS_AS_WARNINGS
import modulecheck.reporting.logging.McLogger
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer

internal class McMessageCollector(
  private val messageRenderer: MessageRenderer,
  private val logger: McLogger,
  private val logLevel: LogLevel
) : MessageCollector {

  private var totalMessages = 0
  private var ignoredMessages = 0

  override fun report(
    severity: CompilerMessageSeverity,
    message: String,
    location: CompilerMessageSourceLocation?
  ) {

    totalMessages++

    when (logLevel) {
      ERRORS -> if (severity.isError) {
        logger.printFailureLine(messageRenderer.render(severity, message, location))
      } else {
        ignoredMessages++
      }

      WARNINGS_AS_ERRORS -> if (severity.isWarning || severity.isError) {
        logger.printFailureLine(messageRenderer.render(severity, message, location))
      } else {
        ignoredMessages++
      }

      WARNINGS_AS_WARNINGS -> when {
        severity.isError -> {
          logger.printFailureLine(messageRenderer.render(severity, message, location))
        }

        severity.isWarning -> {
          logger.printWarningLine(messageRenderer.render(severity, message, location))
        }
      }

      VERBOSE -> when {
        severity.isError -> {
          logger.printFailureLine(messageRenderer.render(severity, message, location))
        }

        severity.isWarning -> {
          logger.printWarningLine(messageRenderer.render(severity, message, location))
        }

        else -> {
          logger.printInfo(messageRenderer.render(severity, message, location))
        }
      }
    }
  }

  override fun clear() {
    ignoredMessages = 0
    totalMessages = 0
  }

  override fun hasErrors(): Boolean = totalMessages > 0

  fun printIssuesCountIfAny() {
    if (ignoredMessages > 0) {
      logger.printWarningLine("Analysis completed with $ignoredMessages ignored issues.")
    }
  }

  enum class LogLevel {
    ERRORS,
    WARNINGS_AS_ERRORS,
    WARNINGS_AS_WARNINGS,
    VERBOSE
  }
}
