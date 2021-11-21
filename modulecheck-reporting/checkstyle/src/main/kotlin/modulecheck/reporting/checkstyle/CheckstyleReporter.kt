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
import org.unbescape.xml.XmlEscape
import java.io.File
import javax.inject.Inject

class CheckstyleReporter @Inject constructor() {

  fun createXml(results: List<FindingResult>): String = buildString {

    appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
    appendLine("<checkstyle version=\"4.3\">")

    results.groupBy { it.buildFile.toUnifiedString() }
      .entries
      .forEach { (filePathString, values) ->

        appendLine("\t<file name=\"${filePathString.xml()}\">")

        values.forEach {

          val row = it.positionOrNull?.row ?: -1
          val column = it.positionOrNull?.column ?: -1

          val severity = if (it.fixed) "info" else "error"
          val source = "modulecheck." + it.problemName

          val line = "\t\t<error line=\"${row.xml()}\" " +
            "column=\"${column.xml()}\" " +
            "severity=\"${severity.xml()}\" " +
            "dependency=\"${it.dependencyPath.xml()}\" " +
            "message=\"${it.message.xml()}\" " +
            "source=\"${source.xml()}\" />"

          appendLine(line)
        }

        appendLine("\t</file>")
      }

    appendLine("</checkstyle>")
  }

  private fun Any.xml() = XmlEscape.escapeXml11(toString().trim())

  private fun File.toUnifiedString(): String = toString().replace(File.separatorChar, '/')
}
