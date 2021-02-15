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
import modulecheck.api.Project2
import modulecheck.api.psi.PsiElementWithSurroundingText
import modulecheck.core.internal.asKtFile
import modulecheck.core.parser.DslBlockParser
import modulecheck.core.rule.AbstractRule
import java.util.*

class SortPluginsFinding(
  override val dependentProject: Project2,
  val parser: DslBlockParser,
  val comparator: Comparator<PsiElementWithSurroundingText>
) : Finding, Fixable {
  override val problemName = "unsorted plugins"

  override val dependencyIdentifier = ""

  override fun positionOrNull(): Position? = null

  override fun fix(): Boolean {
    val result = parser.parse(dependentProject.buildFile.asKtFile()) ?: return false

    val sorted = result
      .elements
      .sortedWith(comparator)
      .joinToString("\n")
      .trim()

    val allText = dependentProject.buildFile.readText()

    val newText = allText.replace(result.blockText, sorted)

    dependentProject.buildFile.writeText(newText)

    return true
  }
}

class SortPluginsRule(
  project: Project2,
  alwaysIgnore: Set<String>,
  ignoreAll: Set<String>,
  val parser: DslBlockParser,
  val comparator: Comparator<PsiElementWithSurroundingText>
) : AbstractRule<SortPluginsFinding>(
  project, alwaysIgnore, ignoreAll
) {
  override fun check(): List<SortPluginsFinding> {
    val result = parser.parse(project.buildFile.asKtFile()) ?: return emptyList()

    val sorted = result
      .elements
      .sortedWith(comparator)
      .joinToString("\n")
      .trim()

    return if (result.blockText == sorted) {
      emptyList()
    } else {
      listOf(SortPluginsFinding(project, parser, comparator))
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
