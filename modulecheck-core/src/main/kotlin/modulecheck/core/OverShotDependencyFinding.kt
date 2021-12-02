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

import modulecheck.parsing.DependenciesBlock
import modulecheck.parsing.DependencyBlockParser
import modulecheck.parsing.ModuleDependencyDeclaration
import modulecheck.project.ConfigurationName
import modulecheck.project.McProject
import org.jetbrains.kotlin.util.prefixIfNot
import java.io.File

data class OverShotDependencyFinding(
  override val dependentPath: String,
  override val buildFile: File,
  override val dependencyProject: McProject,
  override val dependencyIdentifier: String,
  override val configurationName: ConfigurationName,
  val originalConfigurationName: ConfigurationName,
  val isTestFixture: Boolean
) : DependencyFinding("overshot") {

  override val message: String
    get() = "The dependency is not used in the source set for which it is configured, but it is " +
      "used in another source set which inherits from the first.  For example, a test-only " +
      "dependency which is declared via `implementation` instead of `testImplementation`."

  override fun fix(): Boolean {

    val blocks = DependencyBlockParser
      .parse(buildFile)

    val sourceDeclaration = blocks.firstNotNullOfOrNull { block ->

      block.getOrEmpty(dependencyProject.path, originalConfigurationName)
        .firstOrNull()
    } ?: return false

    val positionBlockDeclarationPair = blocks.firstNotNullOfOrNull { block ->

      val match = matchingDeclaration(block) ?: return@firstNotNullOfOrNull null

      block to match
    } ?: return false

    val (block, positionDeclaration) = positionBlockDeclarationPair

    val newDeclaration = sourceDeclaration.replace(configurationName, testFixtures = isTestFixture)

    val oldStatement = positionDeclaration.statementWithSurroundingText
    val newStatement = oldStatement.plus(
      newDeclaration.statementWithSurroundingText
        .prefixIfNot("\n")
    )

    val newBlock = block.contentString.replaceFirst(
      oldValue = oldStatement,
      newValue = newStatement
    )

    val fileText = buildFile.readText()
      .replace(block.contentString, newBlock)

    buildFile.writeText(fileText)

    return true
  }

  private fun matchingDeclaration(block: DependenciesBlock) = block.allDeclarations
    .filterIsInstance<ModuleDependencyDeclaration>()
    .maxByOrNull { declaration -> declaration.configName == configurationName }
    ?: block.allDeclarations
      .filterNot { it is ModuleDependencyDeclaration }
      .maxByOrNull { declaration -> declaration.configName == configurationName }
    ?: block.allDeclarations
      .lastOrNull()

  override fun fromStringOrEmpty(): String = ""

  override fun toString(): String {
    return "OverShotDependency(\n" +
      "\tdependentPath='$dependentPath', \n" +
      "\tbuildFile=$buildFile, \n" +
      "\tdependencyProject=$dependencyProject, \n" +
      "\tdependencyIdentifier='$dependencyIdentifier', \n" +
      "\tconfigurationName=$configurationName\n" +
      ")"
  }
}
