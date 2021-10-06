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

package modulecheck.parsing

abstract class PluginsBlock(var contentString: String) {

  protected val originalLines = contentString.lines().toMutableList()

  private val _allDeclarations = mutableListOf<PluginDeclaration>()

  val allDeclarations: List<PluginDeclaration>
    get() = _allDeclarations

  protected val allBlockStatements = mutableListOf<String>()

  fun addStatement(parsedString: String) {
    val originalString = getOriginalString(parsedString)

    val declaration = PluginDeclaration(
      declarationText = parsedString,
      statementWithSurroundingText = originalString
    )
    _allDeclarations.add(declaration)
  }

  protected abstract fun findOriginalStringIndex(parsedString: String): Int

  private fun getOriginalString(parsedString: String): String {
    val originalStringIndex = findOriginalStringIndex(parsedString)

    val originalStringLines = List(originalStringIndex + 1) {
      originalLines.removeFirst()
    }

    return originalStringLines.joinToString("\n")
  }
}
