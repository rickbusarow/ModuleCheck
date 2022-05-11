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

package modulecheck.finding.internal

import kotlinx.coroutines.runBlocking
import modulecheck.finding.Fixable.Companion
import modulecheck.parsing.gradle.dsl.BuildFileStatement
import modulecheck.parsing.gradle.dsl.DependencyDeclaration
import modulecheck.parsing.gradle.dsl.ModuleDependencyDeclaration
import modulecheck.parsing.gradle.model.ProjectPath
import modulecheck.project.ConfiguredDependency
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject
import modulecheck.utils.isGreaterThan
import modulecheck.utils.prefixIfNot
import modulecheck.utils.replaceDestructured
import modulecheck.utils.sortedWith
import modulecheck.utils.suffixIfNot

fun McProject.addDependency(
  cpd: ConfiguredProjectDependency,
  newDeclaration: DependencyDeclaration,
  existingDeclaration: DependencyDeclaration? = null
) {

  if (existingDeclaration != null) {
    prependStatement(newDeclaration = newDeclaration, existingDeclaration = existingDeclaration)
  } else {
    addStatement(newDeclaration = newDeclaration)
  }

  projectDependencies.add(cpd)
}

/**
 * Finds the existing project dependency declaration (if there are any) which is the closest match
 * to the desired new dependency.
 *
 * @param matchPathFirst If true, matching project paths will be prioritized over matching
 *   configurations. If false, configuration matches will take priority over a matching project path.
 * @return the closest matching declaration, or null if there are no declarations at all.
 */
suspend fun McProject.closestDeclarationOrNull(
  newDependency: ConfiguredProjectDependency,
  matchPathFirst: Boolean
): DependencyDeclaration? {

  return buildFileParser.dependenciesBlocks()
    .firstNotNullOfOrNull { dependenciesBlock ->

      val allModuleDeclarations = dependenciesBlock.settings

      allModuleDeclarations
        .sortedWith(
          {
            if (matchPathFirst) it.configName == newDependency.configurationName
            else it.configName != newDependency.configurationName
          },
          { it !is ModuleDependencyDeclaration },
          { (it as? ModuleDependencyDeclaration)?.projectPath?.value ?: "" }
        )
        .let { sorted ->

          sorted.firstOrNull { it.projectPathOrNull() == newDependency.path }
            ?: sorted.firstOrNull {
              it.projectPathOrNull()?.isGreaterThan(newDependency.path) ?: false
            }
            ?: sorted.lastOrNull()
        }
        ?.let { declaration ->

          val sameProject = declaration.projectPathOrNull() == newDependency.path

          if (sameProject) {
            declaration
          } else {

            val precedingWhitespace = "^\\s*".toRegex()
              .find(declaration.statementWithSurroundingText)?.value ?: ""

            (declaration as? ModuleDependencyDeclaration)?.copy(
              statementWithSurroundingText = declaration.statementWithSurroundingText
                .prefixIfNot(precedingWhitespace),
              suppressed = emptyList()
            )
          }
        }
    }
}

private fun DependencyDeclaration.projectPathOrNull(): ProjectPath? {
  return (this as? ModuleDependencyDeclaration)?.projectPath
}

private fun McProject.prependStatement(
  newDeclaration: DependencyDeclaration,
  existingDeclaration: DependencyDeclaration
) = synchronized(buildFile) {

  val oldStatement = existingDeclaration.statementWithSurroundingText
  val newStatement = newDeclaration.statementWithSurroundingText

  // the `prefixIfNot("\n")` here is important.
  // It needs to match what we're doing if we delete a dependency.  Otherwise, we wind up adding
  // or removing newlines instead of just modifying the dependencies.
  // See https://github.com/RBusarow/ModuleCheck/issues/443
  val combinedStatement = newStatement.plus(oldStatement.prefixIfNot("\n"))

  val buildFileText = buildFile.readText()

  buildFile.writeText(buildFileText.replace(oldStatement, combinedStatement))
}

private fun McProject.addStatement(
  newDeclaration: DependencyDeclaration
) = synchronized(buildFile) {

  val newStatement = newDeclaration.statementWithSurroundingText

  val buildFileText = buildFile.readText()

  runBlocking {
    val oldBlockOrNull = buildFileParser.dependenciesBlocks().lastOrNull()

    if (oldBlockOrNull != null) {

      val newBlock = oldBlockOrNull.fullText
        .replaceDestructured("""([\s\S]*)}(\s*)""".toRegex()) { group1, group2 ->

          val prefix = group1.trim(' ')
            .suffixIfNot("\n")

          "$prefix$newStatement}$group2"
        }

      buildFile.writeText(buildFileText.replace(oldBlockOrNull.fullText, newBlock))
    } else {

      val newBlock = "\n\ndependencies {\n${newStatement.suffixIfNot("\n")}}"
      val newText = buildFileText + newBlock

      buildFile.writeText(newText)
    }
  }
}

fun McProject.removeDependencyWithComment(
  statement: BuildFileStatement,
  fixLabel: String,
  configuredDependency: ConfiguredDependency? = null
) = synchronized(buildFile) {

  val text = buildFile.readText()

  val declarationText = statement.declarationText

  val lines = declarationText.lines()
  val lastIndex = lines.lastIndex
  val newDeclarationText = lines
    .mapIndexed { index: Int, str: String ->

      // don't comment out a blank line
      if (str.isBlank()) return@mapIndexed str

      val commented = str.replace("""(\s*)(\S.*)""".toRegex()) { mr ->
        val (whitespace, code) = mr.destructured
        "$whitespace${Companion.INLINE_COMMENT}$code"
      }

      if (index == lastIndex) {
        commented + fixLabel
      } else {
        commented
      }
    }
    .joinToString("\n")

  val newText = text.replace(declarationText, newDeclarationText)

  buildFile.writeText(newText)

  if (configuredDependency is ConfiguredProjectDependency) {

    projectDependencies.remove(configuredDependency)
  }
}

fun McProject.removeDependencyWithDelete(
  statement: BuildFileStatement,
  configuredDependency: ConfiguredDependency? = null
) = synchronized(buildFile) {
  val text = buildFile.readText()

  buildFile.writeText(
    // the `prefixIfNot("\n")` here is important.
    // It needs to match what we're doing if we add a new dependency.  Otherwise, we wind up adding
    // or removing newlines instead of just modifying the dependencies.
    // See https://github.com/RBusarow/ModuleCheck/issues/443
    text.replaceFirst(statement.statementWithSurroundingText.prefixIfNot("\n"), "")
  )

  if (configuredDependency is ConfiguredProjectDependency) {
    projectDependencies.remove(configuredDependency)
  }
}
