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

interface Fixable : Finding {

  val dependencyIdentifier: String

  override fun logString(): String {
    return "${buildFile.path}: ${positionString()} $problemName: $dependencyIdentifier"
  }

  fun fix(): Boolean = synchronized(buildFile) {
    val text = buildFile.readText()

    return elementOrNull()
      ?.let { elementWithSurroundingText ->

        val newText = elementWithSurroundingText
          .psiElement
          .text
          .lines()
          .mapIndexed { index: Int, str: String ->
            if (index == 0) {
              INLINE_COMMENT + str + fixLabel()
            } else {
              INLINE_COMMENT + str
            }
          }
          .joinToString("\n")

        val psiElement = elementWithSurroundingText.psiElement
        buildFile
          .writeText(
            text.replaceFirst(psiElement.text, newText)
          )

        true
      } ?: false
  }

  fun fixLabel() = "  $FIX_LABEL [$problemName]"

  companion object {

    const val FIX_LABEL = "// ModuleCheck finding"
    const val INLINE_COMMENT = "// "
  }
}
