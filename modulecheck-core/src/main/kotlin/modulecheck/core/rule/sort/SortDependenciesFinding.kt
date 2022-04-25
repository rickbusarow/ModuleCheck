/*
 * Copyright (C) 2021-2022 Rick Busarow
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

import modulecheck.api.finding.Finding
import modulecheck.api.finding.Finding.Position
import modulecheck.api.finding.Fixable
import modulecheck.api.finding.RemovesDependency.RemovalStrategy
import modulecheck.api.rule.RuleName
import modulecheck.parsing.gradle.Declaration
import modulecheck.parsing.gradle.DependenciesBlock
import modulecheck.parsing.gradle.DependencyDeclaration
import modulecheck.parsing.gradle.ProjectPath
import modulecheck.project.McProject
import modulecheck.utils.LazyDeferred
import modulecheck.utils.lazyDeferred
import org.jetbrains.kotlin.util.suffixIfNot
import java.io.File
import java.util.Locale

class SortDependenciesFinding(
  override val ruleName: RuleName,
  override val dependentProject: McProject,
  override val dependentPath: ProjectPath.StringProjectPath,
  override val buildFile: File,
  private val comparator: Comparator<String>
) : Finding, Fixable {

  override val message: String
    get() = "Project/external dependency declarations are not sorted " +
      "according to the defined pattern."

  override val dependencyIdentifier = ""

  override val positionOrNull: LazyDeferred<Position?> = lazyDeferred { null }

  override val declarationOrNull: LazyDeferred<Declaration?> = lazyDeferred { null }

  override val statementTextOrNull: LazyDeferred<String?> = lazyDeferred { null }

  override suspend fun fix(removalStrategy: RemovalStrategy): Boolean {
    var fileText = buildFile.readText()

    dependentProject.buildFileParser
      .dependenciesBlocks()
      .forEach { block ->

        fileText = sortedDependenciesFileText(block, fileText, comparator)
      }

    buildFile.writeText(fileText)

    return true
  }
}

internal fun sortedDependenciesFileText(
  block: DependenciesBlock,
  fileText: String,
  comparator: Comparator<String>
): String {
  val sorted = block.sortedDeclarations(comparator)

  val trimmedContent = block.lambdaContent
    .trimStart('\n')
    .trimEnd()

  val escapedContent = Regex.escape(trimmedContent)

  val blockRegex = "$escapedContent[\\n\\r]*(\\s*)}".toRegex()

  return fileText.replace(blockRegex) { mr ->

    val whitespaceBeforeBrace = mr.destructured.component1()

    "$sorted$whitespaceBeforeBrace}"
  }
}

internal fun DependenciesBlock.sortedDeclarations(
  comparator: Comparator<String>
): String {
  return settings
    .grouped(comparator)
    .joinToString("\n\n") { declarations ->

      declarations
        .sortedBy { declaration ->
          declaration.declarationText.lowercase(Locale.US)
        }
        .joinToString("\n") {
          it.statementWithSurroundingText
            .trimStart('\n')
            .trimEnd()
            .lines()
            .joinToString("\n")
        }
    }
    .suffixIfNot("\n")
}

internal fun List<DependencyDeclaration>.grouped(
  comparator: Comparator<String>
) = groupBy {
  it.declarationText
    .split("[^a-zA-Z-]".toRegex())
    .filterNot { it.isEmpty() }
    .take(2)
    .joinToString("-")
}
  .toSortedMap(comparator)
  .map { it.value }
