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
import modulecheck.core.parse
import modulecheck.parsing.PluginBlockParser
import modulecheck.parsing.PluginDeclaration
import modulecheck.parsing.PluginsBlock
import org.jetbrains.kotlin.util.suffixIfNot
import java.io.File

class SortPluginsFinding(
  override val dependentPath: String,
  override val buildFile: File,
  val comparator: Comparator<PluginDeclaration>
) : Finding, Fixable {

  override val message: String
    get() = "Plugin declarations are not sorted according to the defined pattern."

  override val findingName = "unsortedPlugins"

  override val dependencyIdentifier = ""

  override val positionOrNull: Position? get() = null

  override fun fix(): Boolean = synchronized(buildFile) {
    val block = PluginBlockParser.parse(buildFile) ?: return false

    var fileText = buildFile.readText()

    val sorted = block.sortedPlugins(comparator)

    fileText = fileText.replace(block.contentString, sorted)

    buildFile.writeText(fileText)

    return true
  }
}

internal fun PluginsBlock.sortedPlugins(
  comparator: Comparator<PluginDeclaration>
): String {
  // Groovy parsing has the last whitespace at the end of the contentString block,
  // so it gets chopped off when doing the replacement.
  // Kotlin parsing includes it as part of the wrapping brackets,
  // so there is no newline whitespace in the block.
  // This regex finds whatever trailing whitespace/newline there is and carries it over to the new
  // block.
  val suffix = "(\\s*)\\z".toRegex()
    .find(contentString)
    ?.destructured
    ?.component1()
    ?: ""

  return allDeclarations
    .sortedWith(comparator)
    .joinToString("\n") { it.statementWithSurroundingText }
    .suffixIfNot(suffix)
}
