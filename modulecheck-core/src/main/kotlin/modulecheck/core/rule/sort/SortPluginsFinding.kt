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
import modulecheck.api.Fixable
import modulecheck.core.kotlinBuildFileOrNull
import modulecheck.psi.DslBlockVisitor
import modulecheck.psi.PsiElementWithSurroundingText
import java.io.File
import java.util.*

class SortPluginsFinding(
  override val dependentPath: String,
  override val buildFile: File,
  val visitor: DslBlockVisitor,
  val comparator: Comparator<PsiElementWithSurroundingText>
) : Finding, Fixable {
  override val problemName = "unsorted plugins"

  override val dependencyIdentifier = ""

  override fun positionOrNull(): Finding.Position? = null

  override fun fix(): Boolean = synchronized(buildFile) {
    val kotlinBuildFile = kotlinBuildFileOrNull() ?: return false

    val result = visitor.parse(kotlinBuildFile) ?: return false

    val sorted = result
      .elements
      .sortedWith(comparator)
      .joinToString("\n")
      .trim()

    val allText = buildFile.readText()

    val newText = allText.replace(result.blockText, sorted)

    buildFile.writeText(newText)

    return true
  }
}
