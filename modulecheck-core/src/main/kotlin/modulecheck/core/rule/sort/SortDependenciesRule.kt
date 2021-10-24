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

import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.core.parse
import modulecheck.core.rule.ModuleCheckRule
import modulecheck.parsing.DependenciesBlock
import modulecheck.parsing.DependencyBlockParser
import modulecheck.parsing.DependencyDeclaration
import modulecheck.parsing.Project2
import org.jetbrains.kotlin.util.suffixIfNot
import java.util.*

class SortDependenciesRule(
  override val settings: ModuleCheckSettings
) : ModuleCheckRule<SortDependenciesFinding>() {

  override val id = "SortDependencies"
  override val description = "Sorts all dependencies within a dependencies { ... } block"

  private val elementComparables: Array<(String) -> Comparable<*>> =
    settings
      .sort
      .dependencyComparators
      .map { it.toRegex() }
      .map { regex ->
        { str: String -> !str.matches(regex) }
      }.toTypedArray()

  @Suppress("SpreadOperator")
  private val comparator: Comparator<String> = compareBy(
    *elementComparables,
    { // we have to use `toLowerCase()` for compatibility with Kotlin 1.4.x and Gradle < 7.0
      @Suppress("DEPRECATION")
      it.toLowerCase(Locale.US)
    }
  )

  override fun check(project: Project2): List<SortDependenciesFinding> {
    val allSorted = DependencyBlockParser
      .parse(project.buildFile)
      .all { block ->

        if (block.contentString.isBlank()) return@all true

        val fileText = project.buildFile.readText()

        fileText == sortedDependenciesFileText(block, fileText, comparator)
      }

    return if (allSorted) {
      emptyList()
    } else {
      listOf(SortDependenciesFinding(project.path, project.buildFile, comparator))
    }
  }
}

fun List<DependencyDeclaration>.grouped(
  comparator: Comparator<String>
) = groupBy {
  it.declarationText
    .split("[^a-zA-Z-]".toRegex())
    .filterNot { it.isEmpty() }
    .take(2)
    .joinToString("-")
}
  .toSortedMap(comparator)
  .map { it.value }

internal fun DependenciesBlock.sortedDeclarations(
  comparator: Comparator<String>
): String {
  return allDeclarations
    .grouped(comparator)
    .joinToString("\n\n") { declarations ->

      declarations
        .sortedBy { declaration ->
          // we have to use `toLowerCase()` for compatibility with Kotlin 1.4.x and Gradle < 7.0
          @Suppress("DEPRECATION")
          declaration.declarationText.toLowerCase(Locale.US)
        }
        .joinToString("\n") {
          it.statementWithSurroundingText
            .trimStart('\n')
            .trimEnd()
            .lines()
            .joinToString("\n")
        }
    }
    .suffixIfNot("\n")
}
