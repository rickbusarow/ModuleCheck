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
import modulecheck.api.psi.PsiElementWithSurroundingText
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.core.rule.AbstractRule
import modulecheck.psi.DslBlockVisitor
import java.util.*

class SortPluginsRule(
  override val settings: ModuleCheckSettings
) : AbstractRule<SortPluginsFinding>() {

  override val id = "SortPlugins"

  private val comparables: Array<(PsiElementWithSurroundingText) -> Comparable<*>> =
    settings
      .sortSettings
      .dependencyComparators
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
      listOf(SortPluginsFinding(project.buildFile, visitor, comparator))
    }
  }

  companion object {
    val patterns = listOf(
      """id\("com\.android.*"\)""",
      """id\("android-.*"\)""",
      """id\("java-library"\)""",
      """kotlin\("jvm"\)""",
      """android.*""",
      """javaLibrary.*""",
      """kotlin.*""",
      """id.*"""
    )
  }
}
