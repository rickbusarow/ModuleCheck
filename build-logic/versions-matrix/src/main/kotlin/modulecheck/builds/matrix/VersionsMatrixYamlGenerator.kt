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

package modulecheck.builds.matrix

import modulecheck.builds.matrix.VersionsMatrix.Exclusion

internal class VersionsMatrixYamlGenerator {

  fun generate(versionsMatrix: VersionsMatrix, indentSize: Int): String {
    var currentIndent = " ".repeat(indentSize)

    fun StringBuilder.indent(content: StringBuilder.() -> Unit) {
      currentIndent += "  "
      content()
      currentIndent = currentIndent.removeSuffix("  ")
    }

    fun StringBuilder.line(content: String) {
      appendLine(currentIndent + content)
    }

    val yaml = buildString {
      line("matrix:")

      indent {
        line("kotlin-version: ${versionsMatrix.kotlinList.asYamlList()}")
        line("gradle-version: ${versionsMatrix.gradleList.asYamlList()}")
        line("agp-version: ${versionsMatrix.agpList.asYamlList()}")
        line("anvil-version: ${versionsMatrix.anvilList.asYamlList()}")

        if (versionsMatrix.exclusions.isEmpty()) {
          line("exclude: [ ]")
        } else {
          line("exclude:")

          indent {
            versionsMatrix.exclusions
              .forEach { exclude ->
                exclude.asYamlLines()
                  .forEach { line ->
                    line(line)
                  }
              }
          }
        }
      }
    }
    return yaml
  }

  private fun List<String>.asYamlList() = joinToString(", ", "[ ", " ]") { it }

  private fun Exclusion.asYamlLines(): List<String> {
    return listOf(
      "gradle-version" to gradle,
      "agp-version" to agp,
      "kotlin-version" to kotlin,
      "anvil-version" to anvil
    )
      .filter { it.second != null }
      .mapIndexed { index, pair ->
        val (label, value) = pair
        if (index == 0) {
          "- $label: $value"
        } else {
          "  $label: $value"
        }
      }
  }
}
