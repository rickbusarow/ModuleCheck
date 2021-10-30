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
  return allDeclarations
    .sortedWith(comparator)
    .joinToString("\n") { it.statementWithSurroundingText }
}
