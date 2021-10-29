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

interface Logger {

  fun printReport(report: Report)

  fun printHeader(message: String)
  fun printWarning(message: String)
  fun printWarningLine(message: String)
  fun printInfo(message: String)
  fun printFailure(message: String)
  fun printFailureLine(message: String)
  fun printFailureHeader(message: String)
  fun printSuccess(message: String)
  fun printSuccessLine(message: String)
  fun printSuccessHeader(message: String)
}
