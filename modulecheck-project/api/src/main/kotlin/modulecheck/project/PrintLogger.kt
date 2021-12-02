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

package modulecheck.project

class PrintLogger : Logger {
  override fun printReport(report: Report) {
    println(report.joinToString())
  }

  override fun printHeader(message: String) = println(message)

  override fun printWarning(message: String) = print(message)

  override fun printWarningLine(message: String) = println(message)

  override fun printInfo(message: String) = println(message)

  override fun printFailure(message: String) = print(message)

  override fun printFailureLine(message: String) = println(message)

  override fun printFailureHeader(message: String) = println(message)

  override fun printSuccess(message: String) = print(message)

  override fun printSuccessLine(message: String) = println(message)

  override fun printSuccessHeader(message: String) = println(message)
}
