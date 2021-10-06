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
import modulecheck.parsing.DependencyBlockParser
import modulecheck.parsing.DependencyDeclaration
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

        val sorted = block.sortedDeclarations(comparator)

        fileText = fileText.replace(block.contentString, sorted)
      }

    buildFile.writeText(fileText)

    return true
  }
}

fun List<DependencyDeclaration>.grouped(
  comparator: Comparator<String>
) = groupBy {
  it.declarationText
    .split("[(.]".toRegex())
    .take(2)
    .joinToString("-")
}
  .toSortedMap(comparator)
  .map { it.value }
