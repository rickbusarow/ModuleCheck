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

package modulecheck.api

import modulecheck.psi.PsiElementWithSurroundingText
import java.io.File

interface Finding {

  val problemName: String
  val dependentPath: String
  val buildFile: File

  fun logString(): String {
    return "${buildFile.path}: ${positionString()} $problemName"
  }

  fun elementOrNull(): PsiElementWithSurroundingText? = null
  fun positionOrNull(): Position?
  fun positionString() = positionOrNull()?.logString() ?: ""

  data class Position(
    val row: Int,
    val column: Int
  ) {
    fun logString(): String = "($row, $column): "
  }
}

interface Fixable : Finding {

  val dependencyIdentifier: String

  override fun logString(): String {
    return "${buildFile.path}: ${positionString()} $problemName: $dependencyIdentifier"
  }

  fun fix(): Boolean {
    val text = buildFile.readText()

    return elementOrNull()
      ?.psiElement
      ?.let { element ->

        val newText = element.text
          .lines()
          .mapIndexed { index: Int, str: String ->
            if (index == 0) {
              INLINE_COMMENT + str + fixLabel()
            } else {
              INLINE_COMMENT + str
            }
          }
          .joinToString("\n")

        buildFile
          .writeText(text.replace(element.text, newText))

        true
      } ?: false
  }

  fun fixLabel() = "  $FIX_LABEL [$problemName]"

  companion object {

    const val FIX_LABEL = "// ModuleCheck finding"
    const val INLINE_COMMENT = "// "
  }
}
