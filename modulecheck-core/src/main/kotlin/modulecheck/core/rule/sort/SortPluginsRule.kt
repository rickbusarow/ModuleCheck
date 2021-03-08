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

import modulecheck.api.Project2
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.core.rule.ModuleCheckRule
import modulecheck.psi.DslBlockVisitor
import modulecheck.psi.PsiElementWithSurroundingText
import java.util.*

class SortPluginsRule(
  override val settings: ModuleCheckSettings
) : ModuleCheckRule<SortPluginsFinding>() {

  override val id = "SortPlugins"
  override val description =
    "Sorts Gradle plugins which are applied using the plugins { ... } block"

  private val comparables: Array<(PsiElementWithSurroundingText) -> Comparable<*>> =
    settings
      .sort
      .pluginComparators
      .map { it.toRegex() }
      .map { regex ->
        { str: String -> !str.matches(regex) }
      }
      .map { booleanLambda ->
        { psi: PsiElementWithSurroundingText ->

          booleanLambda.invoke(psi.psiElement.text)
        }
      }.toTypedArray()

  @Suppress("SpreadOperator")
  private val comparator: Comparator<PsiElementWithSurroundingText> = compareBy(*comparables)

  override fun check(project: Project2): List<SortPluginsFinding> {
    val visitor = DslBlockVisitor("plugins")

    val kotlinBuildFile = project.kotlinBuildFileOrNull() ?: return emptyList()

    val result = visitor.parse(kotlinBuildFile) ?: return emptyList()

    val sorted = result
      .elements
      .sortedWith(comparator)
      .joinToString("\n")
      .trim()

    return if (result.blockText == sorted) {
      emptyList()
    } else {
      listOf(SortPluginsFinding(project.path, project.buildFile, visitor, comparator))
    }
  }
}
