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

interface Fixable : Finding {

  val dependencyIdentifier: String

  override fun logString(): String {
    return "${buildFile.path}: ${positionString()} $problemName: $dependencyIdentifier"
  }

  fun fix(): Boolean = synchronized(buildFile) {
    val text = buildFile.readText()

    val statementText = statementTextOrNull ?: return false

    val lines = statementText.lines()
    val lastIndex = lines.lastIndex
    val newText = lines
      .mapIndexed { index: Int, str: String ->

        // don't comment out a blank line
        if (str.isBlank()) return@mapIndexed str

        val commented = str.replace("""(\s*)(\S.*)""".toRegex()) { mr ->
          val (whitespace, code) = mr.destructured
          "$whitespace$INLINE_COMMENT$code"
        }

        if (index == lastIndex) {
          commented + fixLabel()
        } else {
          commented
        }
      }
      .joinToString("\n")

    buildFile
      .writeText(
        text.replaceFirst(statementText, newText)
      )

    true
  }

  fun fixLabel() = "  $FIX_LABEL [$problemName]"

  companion object {

    const val FIX_LABEL = "// ModuleCheck finding"
    const val INLINE_COMMENT = "// "
  }
}
