/*
 * Copyright (C) 2021-2022 Rick Busarow
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

package modulecheck.core.rule

import modulecheck.api.rule.RuleName
import modulecheck.api.rule.SortRule
import modulecheck.api.settings.ChecksSettings
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.core.rule.sort.SortDependenciesFinding
import modulecheck.core.rule.sort.sortedDependenciesFileText
import modulecheck.project.McProject
import java.util.Locale

class SortDependenciesRule(
  settings: ModuleCheckSettings
) : DocumentedRule<SortDependenciesFinding>(), SortRule<SortDependenciesFinding> {

  override val name = RuleName("sort-dependencies")
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
    { it.lowercase(Locale.US) }
  )

  override suspend fun check(project: McProject): List<SortDependenciesFinding> {
    val allSorted = project.buildFileParser
      .dependenciesBlocks()
      .all { block ->

        if (block.lambdaContent.isBlank()) return@all true

        val fileText = project.buildFile.readText()

        fileText == sortedDependenciesFileText(block, fileText, comparator)
      }

    return if (allSorted) {
      emptyList()
    } else {
      listOf(
        SortDependenciesFinding(
          ruleName = name,
          dependentProject = project,
          dependentPath = project.path,
          buildFile = project.buildFile,
          comparator = comparator
        )
      )
    }
  }

  override fun shouldApply(checksSettings: ChecksSettings): Boolean {
    return checksSettings.sortDependencies
  }
}
