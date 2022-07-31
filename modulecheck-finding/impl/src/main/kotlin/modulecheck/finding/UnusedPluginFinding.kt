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

package modulecheck.finding

import modulecheck.finding.Finding.Position
import modulecheck.finding.RemovesDependency.RemovalStrategy
import modulecheck.finding.internal.removeDependencyWithComment
import modulecheck.finding.internal.removeDependencyWithDelete
import modulecheck.model.dependency.PluginAccessor
import modulecheck.model.dependency.PluginDefinition
import modulecheck.model.dependency.PluginDependency
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.parsing.gradle.dsl.BuildFileStatement
import modulecheck.project.McProject
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.lazyDeferred
import java.io.File

data class UnusedPluginFinding(
  override val dependentProject: McProject,
  override val dependentPath: StringProjectPath,
  override val buildFile: File,
  override val findingName: FindingName,
  val pluginDefinition: PluginDefinition
) : Finding, Problem, Fixable, Deletable {

  override val message: String
    get() = "The `${pluginDefinition.qualifiedId}` plugin dependency declared, " +
      "but no processor dependencies are declared."

  override val dependencyIdentifier = pluginDefinition.qualifiedId

  override val isSuppressed: LazyDeferred<Boolean> = lazyDeferred {
    dependentProject.getSuppressions()
      .get(findingName)
      .any { (it as? PluginDependency)?.accessor in pluginDefinition.accessors }
  }

  override val positionOrNull: LazyDeferred<Position?> = lazyDeferred {
    val text = buildFile
      .readText()

    val lines = text.lines()

    val row = lines
      .indexOfFirst { line ->

        pluginDefinition.accessors.contains(PluginAccessor(line.trim())) ||
          line.contains("plugin = \"${pluginDefinition.qualifiedId}\")") ||
          (
            pluginDefinition.legacyIdOrNull != null &&
              line.contains("plugin = \"${pluginDefinition.legacyIdOrNull}\")")
            )
      }

    if (row < 0) return@lazyDeferred null

    val col = lines[row]
      .indexOfFirst { it != ' ' }

    Position(row + 1, col + 1)
  }

  override val statementOrNull: LazyDeferred<BuildFileStatement?> = lazyDeferred {

    pluginDefinition.accessors
      .firstNotNullOfOrNull { id ->
        dependentProject.buildFileParser.pluginsBlock()?.getById(id.text)
      }
  }
  override val statementTextOrNull: LazyDeferred<String?> = lazyDeferred {
    statementOrNull.await()?.statementWithSurroundingText
  }

  override suspend fun fix(removalStrategy: RemovalStrategy): Boolean {

    val declaration = statementOrNull.await() ?: return false

    dependentProject.removeDependencyWithComment(declaration, fixLabel())

    return true
  }

  override suspend fun delete(): Boolean {

    val declaration = statementOrNull.await() ?: return false

    dependentProject.removeDependencyWithDelete(declaration)

    return true
  }
}
