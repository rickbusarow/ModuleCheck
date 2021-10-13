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

package modulecheck.core.rule.sort

import modulecheck.api.Finding
import modulecheck.api.Finding.Position
import modulecheck.api.Fixable
import modulecheck.core.parse
import modulecheck.parsing.DependenciesBlock
import modulecheck.parsing.DependencyBlockParser
import java.io.File

class SortDependenciesFinding(
  override val dependentPath: String,
  override val buildFile: File,
  private val comparator: Comparator<String>
) : Finding, Fixable {
  override val problemName = "unsorted dependencies"

  override val dependencyIdentifier = ""

  override val positionOrNull: Position? get() = null

  override fun fix(): Boolean = synchronized(buildFile) {
    var fileText = buildFile.readText()

    DependencyBlockParser
      .parse(buildFile)
      .forEach { block ->

        fileText = sortedDependenciesFileText(block, fileText, comparator)
      }

    buildFile.writeText(fileText)

    return true
  }
}

internal fun sortedDependenciesFileText(
  block: DependenciesBlock,
  fileText: String,
  comparator: Comparator<String>
): String {
  val sorted = block.sortedDeclarations(comparator)

  val trimmedContent = block.contentString
    .trimStart('\n')
    .trimEnd()

  val escapedContent = Regex.escape(trimmedContent)

  val blockRegex = "$escapedContent[\\n\\r]*(\\s*)}".toRegex()

  return fileText.replace(blockRegex) { mr ->

    val whitespaceBeforeBrace = mr.destructured.component1()

    "$sorted$whitespaceBeforeBrace}"
  }
}
