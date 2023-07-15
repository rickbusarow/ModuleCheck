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

package modulecheck.builds

import com.github.difflib.text.DiffRow.Tag
import com.github.difflib.text.DiffRowGenerator
import modulecheck.builds.Color.Companion.colorized
import modulecheck.builds.Color.LIGHT_GREEN
import modulecheck.builds.Color.LIGHT_YELLOW

fun diffString(oldStr: String, newStr: String): String {

  return buildString {

    val rows = DiffRowGenerator.create()
      .showInlineDiffs(true)
      .inlineDiffByWord(true)
      .oldTag { _: Boolean? -> "" }
      .newTag { _: Boolean? -> "" }
      .build()
      .generateDiffRows(oldStr.lines(), newStr.lines())

    val linePadding = rows.size.toString().length + 1

    rows.forEachIndexed { line, diffRow ->
      if (diffRow.tag != Tag.EQUAL) {
        append("line ${line.inc().toString().padEnd(linePadding)} ")
      }

      if (diffRow.tag == Tag.CHANGE || diffRow.tag == Tag.DELETE) {
        appendLine("--  ${diffRow.oldLine}".colorized(LIGHT_YELLOW))
      }
      if (diffRow.tag == Tag.CHANGE) {
        append("      " + " ".repeat(linePadding))
      }
      if (diffRow.tag == Tag.CHANGE || diffRow.tag == Tag.INSERT) {
        appendLine("++  ${diffRow.newLine}".colorized(LIGHT_GREEN))
      }
    }
  }
}

@Suppress("MagicNumber")
internal enum class Color(val code: Int) {
  LIGHT_GREEN(92),
  LIGHT_YELLOW(93);

  companion object {

    private val supported = "win" !in System.getProperty("os.name").lowercase()

    fun String.colorized(color: Color) = if (supported) {
      "\u001B[${color.code}m$this\u001B[0m"
    } else {
      this
    }
  }
}
