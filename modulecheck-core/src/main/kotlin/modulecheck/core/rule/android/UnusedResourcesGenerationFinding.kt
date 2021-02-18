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

package modulecheck.core.rule.android

import modulecheck.api.Finding
import modulecheck.api.Fixable
import modulecheck.api.Project2
import modulecheck.api.psi.PsiElementWithSurroundingText
import modulecheck.psi.AndroidBuildFeaturesVisitor
import modulecheck.psi.internal.asKtsFileOrNull

data class UnusedResourcesGenerationFinding(
  override val dependentProject: Project2
) : Finding, Fixable {

  override val problemName = "unused R file generation"

  override val dependencyIdentifier = ""

  override fun elementOrNull(): PsiElementWithSurroundingText? {
    val buildFile = dependentProject.buildFile.asKtsFileOrNull() ?: return null

    return AndroidBuildFeaturesVisitor().find(buildFile, "androidResources")
  }

  override fun positionOrNull(): Finding.Position? {
    val ktFile = dependentProject.buildFile.asKtsFileOrNull() ?: return null

    return androidBlockParser.parse(ktFile)?.let { result ->

      val token = result
        .blockText
        .lines()
        .firstOrNull { it.isNotEmpty() } ?: return@let null

      val lines = ktFile.text.lines()

      val startRow = lines.indexOfFirst { it.matches(androidBlockRegex) }

      if (startRow == -1) return@let null

      val after = lines.subList(startRow, lines.lastIndex)

      val row = after.indexOfFirst { it.contains(token) }

      Finding.Position(row + startRow + 1, 0)
    }
  }

  override fun fix(): Boolean {
    val ktFile = dependentProject.buildFile.asKtsFileOrNull() ?: return false

    val oldBlock = elementOrNull()?.toString() ?: return false

    val newBlock = oldBlock.replace("true", "false")

    val oldText = ktFile.text

    dependentProject.buildFile.writeText(oldText.replace(oldBlock, newBlock))

    return true
  }
}
