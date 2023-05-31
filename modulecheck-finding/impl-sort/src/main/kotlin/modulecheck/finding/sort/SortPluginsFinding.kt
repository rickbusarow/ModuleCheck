/*
 * Copyright (C) 2021-2023 Rick Busarow
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

package modulecheck.finding.sort

import modulecheck.finding.Finding
import modulecheck.finding.Finding.Position
import modulecheck.finding.FindingName
import modulecheck.finding.Fixable
import modulecheck.finding.RemovesDependency.RemovalStrategy
import modulecheck.model.dependency.ProjectPath
import modulecheck.parsing.gradle.dsl.BuildFileStatement
import modulecheck.parsing.gradle.dsl.PluginDeclaration
import modulecheck.parsing.gradle.dsl.PluginsBlock
import modulecheck.project.McProject
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.lazyDeferred
import modulecheck.utils.suffixIfNot
import java.io.File

class SortPluginsFinding(
  override val dependentProject: McProject,
  override val dependentPath: ProjectPath.StringProjectPath,
  override val buildFile: File,
  val comparator: Comparator<PluginDeclaration>
) : Finding, Fixable {

  override val findingName: FindingName = NAME

  override val message: String
    get() = "Plugin declarations are not sorted according to the defined pattern."

  override val dependencyIdentifier: String = ""

  override val positionOrNull: LazyDeferred<Position?> = lazyDeferred { null }

  override val statementOrNull: LazyDeferred<BuildFileStatement?> = lazyDeferred { null }

  override val statementTextOrNull: LazyDeferred<String?> = lazyDeferred { null }

  override suspend fun fix(removalStrategy: RemovalStrategy): Boolean {
    val block = dependentProject.buildFileParser
      .pluginsBlock() ?: return false

    var fileText = buildFile.readText()

    val sorted = block.sortedPlugins(comparator)

    fileText = fileText.replace(block.lambdaContent, sorted)

    buildFile.writeText(fileText)

    return true
  }

  companion object {
    val NAME: FindingName = FindingName("sort-plugins")
  }
}

fun PluginsBlock.sortedPlugins(comparator: Comparator<PluginDeclaration>): String {
  // Groovy parsing has the last whitespace at the end of the contentString block,
  // so it gets chopped off when doing the replacement.
  // Kotlin parsing includes it as part of the wrapping brackets,
  // so there is no newline whitespace in the block.
  // This regex finds whatever trailing whitespace/newline there is and carries it over to the new
  // block.
  val suffix = "(\\s*)\\z".toRegex()
    .find(lambdaContent)
    ?.destructured
    ?.component1()
    .orEmpty()

  return settings
    .sortedWith(comparator)
    .joinToString("\n") { it.statementWithSurroundingText }
    .suffixIfNot(suffix)
}
