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

package modulecheck.core

import modulecheck.api.finding.Deletable
import modulecheck.api.finding.Finding
import modulecheck.api.finding.Finding.Position
import modulecheck.api.finding.Fixable
import modulecheck.api.finding.Problem
import modulecheck.api.finding.RemovesDependency.RemovalStrategy
import modulecheck.api.finding.internal.removeDependencyWithComment
import modulecheck.api.finding.internal.removeDependencyWithDelete
import modulecheck.parsing.gradle.Declaration
import modulecheck.parsing.gradle.ProjectPath.StringProjectPath
import modulecheck.project.McProject
import modulecheck.utils.LazyDeferred
import modulecheck.utils.lazyDeferred
import java.io.File

data class UnusedPluginFinding(
  override val dependentProject: McProject,
  override val dependentPath: StringProjectPath,
  override val buildFile: File,
  override val findingName: String,
  val pluginId: String,
  val alternatePluginId: String = "",
  val kotlinPluginFunction: String = ""
) : Finding, Problem, Fixable, Deletable {

  override val message: String
    get() = "The `$pluginId` plugin dependency declared, " +
      "but no processor dependencies are declared."

  override val dependencyIdentifier = pluginId

  override val positionOrNull: LazyDeferred<Position?> = lazyDeferred {
    val text = buildFile
      .readText()

    val lines = text.lines()

    val row = lines
      .indexOfFirst { line ->
        line.contains("id(\"$pluginId\")") ||
          line.contains("id(\"$alternatePluginId\")") ||
          line.contains(kotlinPluginFunction) ||
          line.contains("plugin = \"$pluginId\")") ||
          line.contains("plugin = \"$alternatePluginId\")")
      }

    if (row < 0) return@lazyDeferred null

    val col = lines[row]
      .indexOfFirst { it != ' ' }

    Position(row + 1, col + 1)
  }

  override val declarationOrNull: LazyDeferred<Declaration?> = lazyDeferred {

    sequenceOf(
      "id(\"$pluginId\")",
      "id \"$pluginId\"",
      "id '$pluginId'",
      "id(\"$alternatePluginId\")",
      "id \"$alternatePluginId\"",
      "id '$alternatePluginId'",
      kotlinPluginFunction
    ).firstNotNullOfOrNull { id ->
      dependentProject.buildFileParser.pluginsBlock()?.getById(id)
    }
  }
  override val statementTextOrNull: LazyDeferred<String?> = lazyDeferred {
    declarationOrNull.await()?.statementWithSurroundingText
  }

  override suspend fun fix(removalStrategy: RemovalStrategy): Boolean {

    val declaration = declarationOrNull.await() ?: return false

    dependentProject.removeDependencyWithComment(declaration, fixLabel())

    return true
  }

  override suspend fun delete(): Boolean {

    val declaration = declarationOrNull.await() ?: return false

    dependentProject.removeDependencyWithDelete(declaration)

    return true
  }
}
