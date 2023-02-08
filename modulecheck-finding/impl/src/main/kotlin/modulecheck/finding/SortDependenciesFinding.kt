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

package modulecheck.finding

import modulecheck.finding.Finding.Position
import modulecheck.finding.RemovesDependency.RemovalStrategy
import modulecheck.model.dependency.ProjectPath
import modulecheck.parsing.gradle.dsl.BuildFileStatement
import modulecheck.parsing.gradle.dsl.DependenciesBlock
import modulecheck.parsing.gradle.dsl.DependencyDeclaration
import modulecheck.project.McProject
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.lazyDeferred
import modulecheck.utils.suffixIfNot
import java.io.File
import java.util.Locale

class SortDependenciesFinding(
  override val dependentProject: McProject,
  override val dependentPath: ProjectPath.StringProjectPath,
  override val buildFile: File,
  private val comparator: Comparator<String>
) : Finding, Fixable {
  override val findingName: FindingName = NAME

  override val message: String
    get() = "Project/external dependency declarations are not sorted " +
      "according to the defined pattern."

  override val dependencyIdentifier: String = ""

  override val positionOrNull: LazyDeferred<Position?> = lazyDeferred { null }

  override val statementOrNull: LazyDeferred<BuildFileStatement?> = lazyDeferred { null }

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

  companion object {
    val NAME: FindingName = FindingName("sort-dependencies")
  }
}

fun sortedDependenciesFileText(
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
) = groupBy { declaration ->
  declaration
    .declarationText
    .split("[^a-zA-Z-]".toRegex())
    .filterNot { it.isEmpty() }
    .take(2)
    .joinToString("-")
}
  .toSortedMap(comparator)
  .map { it.value }
