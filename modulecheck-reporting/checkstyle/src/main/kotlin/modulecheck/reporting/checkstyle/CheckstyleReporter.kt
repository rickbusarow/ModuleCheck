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

package modulecheck.reporting.checkstyle

import modulecheck.finding.Finding.FindingResult
import modulecheck.utils.indent
import org.unbescape.xml.XmlEscape
import java.io.File
import javax.inject.Inject

class CheckstyleReporter @Inject constructor() {

  fun createXml(results: List<FindingResult>): String = buildString {

    appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
    appendLine("""<checkstyle version="4.3">""")

    results.groupBy { it.buildFile.toUnifiedString() }
      .entries
      .forEach { (filePathString, values) ->

        indent {

          appendLine("""<file name="${filePathString.xml()}">""")

          values.forEach { findingResult ->

            val row = findingResult.positionOrNull?.row ?: -1
            val column = findingResult.positionOrNull?.column ?: -1

            val severity = if (findingResult.fixed) "info" else "error"
            val source = "modulecheck." + findingResult.findingName.id

            indent {
              append("""<error line="${row.xml()}"""")
              append(""" column="${column.xml()}"""")
              append(""" severity="${severity.xml()}"""")
              append(""" dependency="${findingResult.dependencyIdentifier.xml()}"""")
              append(""" message="${findingResult.message.xml()}"""")
              append(""" source="${source.xml()}" />""")
              appendLine()
            }
          }

          appendLine("</file>")
        }
      }

    appendLine("</checkstyle>")
  }

  private fun Any.xml() = XmlEscape.escapeXml11(toString().trim())

  private fun File.toUnifiedString(): String = toString().replace(File.separatorChar, '/')
}
