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

package modulecheck.reporting.checkstyle

import modulecheck.api.Finding.FindingResult
import modulecheck.api.Reporter
import org.unbescape.xml.XmlEscape
import java.io.File

class CheckStyleReporter : Reporter<String> {

  override fun reportResults(results: List<FindingResult>): String {

    val lines = ArrayList<String>()
    lines += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    lines += "<checkstyle version=\"4.3\">"

    results.groupBy { it.buildFile.toUnifiedString() }
      .entries
      .forEach { (filePathString, values) ->

        lines += "<file name=\"${filePathString.xml()}\">"

        values.forEach {

          val row = it.positionOrNull?.row ?: -1
          val column = it.positionOrNull?.column ?: -1

          val severity = if (it.fixed) "info" else "error"
          val source = "modulecheck." + it.problemName

          val line = arrayOf(
            "\t<error line=\"${row.xml()}\"",
            "column=\"${column.xml()}\"",
            "severity=\"${severity.xml()}\"",
            "message=\"${it.message.xml()}\"",
            "source=\"${source.xml()}\" />"
          ).joinToString(separator = " ")

          lines += line
        }

        lines += "</file>"
      }

    lines += "</checkstyle>"
    return lines.joinToString(separator = "\n")
  }

  private fun Any.xml() = XmlEscape.escapeXml11(toString().trim())

  fun File.toUnifiedString(): String = toString().replace(File.separatorChar, '/')
}
