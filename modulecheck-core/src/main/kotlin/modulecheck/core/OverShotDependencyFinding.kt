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

import modulecheck.core.context.OverShotDependencies
import modulecheck.parsing.*
import modulecheck.parsing.ModuleRef.StringRef
import modulecheck.parsing.ModuleRef.TypeSafeRef
import java.io.File

data class OverShotDependencyFinding(
  override val dependentPath: String,
  override val buildFile: File,
  override val dependencyProject: Project2,
  override val dependencyIdentifier: String,
  override val configurationName: ConfigurationName
) : DependencyFinding("overshot") {

  override fun fix(): Boolean {

    val blocks = DependencyBlockParser
      .parse(buildFile)

    val blockMatchPairOrNull = blocks.firstNotNullOfOrNull { block ->

      val match = matchingDeclaration(block) ?: return@firstNotNullOfOrNull null

      block to match
    }

    if (blockMatchPairOrNull != null) {

      val (block, match) = blockMatchPairOrNull

      val moduleDeclaration = newModuleDeclaration(match)

      val newDeclaration = newDeclarationText(match, moduleDeclaration)

      val oldDeclarationLine = match.statementWithSurroundingText
        .lines()
        .first { it.contains(match.declarationText.lines().first()) }

      val indent = "(\\s*)".toRegex()
        .find(oldDeclarationLine)
        ?.destructured
        ?.component1()
        ?: "  "

      val newBlock = block.contentString.replace(
        match.declarationText,
        match.declarationText + "\n$indent" + newDeclaration
      )

      val fileText = buildFile.readText()
        .replace(block.contentString, newBlock)

      buildFile.writeText(fileText)
      return true
    }

    return false
  }

  private fun matchingDeclaration(block: DependenciesBlock) = block.allDeclarations
    .filterIsInstance<ModuleDependencyDeclaration>()
    .maxByOrNull { declaration -> declaration.configName == configurationName }
    ?: block.allDeclarations
      .filterNot { it is ModuleDependencyDeclaration }
      .maxByOrNull { declaration -> declaration.configName == configurationName }
    ?: block.allDeclarations
      .lastOrNull()

  private fun newModuleDeclaration(match: DependencyDeclaration) = when (match) {
    is ExternalDependencyDeclaration -> dependencyProject.path
    is ModuleDependencyDeclaration -> when (match.moduleRef) {
      is StringRef -> dependencyProject.path
      is TypeSafeRef -> StringRef(dependencyProject.path).toTypeSafe().value
    }
    is UnknownDependencyDeclaration -> dependencyProject.path
  }

  private fun newDeclarationText(match: DependencyDeclaration, moduleDeclaration: String): String {
    return match.declarationText
      .replace(match.configName.value, configurationName.value)
      .let {
        when (match) {
          is ExternalDependencyDeclaration -> it.replace("""(["']).*(["'])""".toRegex()) { mr ->
            val quotes = mr.destructured.component1()

            "project($quotes$moduleDeclaration$quotes)"
          }
          is ModuleDependencyDeclaration -> it.replace(match.moduleRef.value, moduleDeclaration)
          is UnknownDependencyDeclaration -> it.replace(
            match.argument,
            "project(\"$moduleDeclaration\")"
          )
        }
      }
  }

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

val ProjectContext.overshotDependencies: OverShotDependencies get() = get(OverShotDependencies)
