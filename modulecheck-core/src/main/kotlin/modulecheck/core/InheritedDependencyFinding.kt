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

package modulecheck.core

import modulecheck.api.Finding.Position
import modulecheck.core.internal.positionIn
import modulecheck.parsing.ConfigurationName
import modulecheck.parsing.ConfiguredProjectDependency
import modulecheck.parsing.DependencyBlockParser
import modulecheck.parsing.McProject
import org.jetbrains.kotlin.util.prefixIfNot
import java.io.File

data class InheritedDependencyFinding(
  override val dependentPath: String,
  val dependentProject: McProject,
  override val buildFile: File,
  override val dependencyProject: McProject,
  val dependencyPath: String,
  override val configurationName: ConfigurationName,
  val source: ConfiguredProjectDependency
) : DependencyFinding("inheritedDependency"),
  Comparable<InheritedDependencyFinding> {

  override val message: String
    get() = "Transitive dependencies which are directly referenced should be declared in this module."

  override val dependencyIdentifier = dependencyPath + fromStringOrEmpty()

  override val positionOrNull: Position? by lazy {
    source.project.positionIn(buildFile, source.configurationName)
  }

  override fun fromStringOrEmpty(): String {
    return if (dependencyProject.path == source.project.path) {
      ""
    } else {
      source.project.path
    }
  }

  override fun fix(): Boolean = synchronized(buildFile) {
    val fromPath = source.project.path

    val blocks = DependencyBlockParser.parse(buildFile)

    val (block, match) = blocks.firstNotNullOfOrNull { block ->
      block to block.getOrEmpty(fromPath, source.configurationName)
    }
      ?.let { (block, declarations) ->

        val matchStatement = declarations.firstOrNull()
          ?: return false

        block to matchStatement
      }
      ?: return false

    val newDeclaration = match.replace(
      configName = configurationName, modulePath = dependencyPath
    )

    val oldStatement = match.statementWithSurroundingText
    val newStatement = newDeclaration.statementWithSurroundingText
      .plus(oldStatement.prefixIfNot("\n"))

    val newDependencies = block.contentString.replaceFirst(
      oldValue = oldStatement,
      newValue = newStatement
    )

    val text = buildFile.readText()

    val newText = text.replaceFirst(block.contentString, newDependencies)

    buildFile.writeText(newText)

    return true
  }

  override fun compareTo(other: InheritedDependencyFinding): Int {

    return compareBy<InheritedDependencyFinding>(
      { it.configurationName },
      { it.source.isTestFixture },
      { it.dependencyPath }
    ).compare(this, other)
  }
}
