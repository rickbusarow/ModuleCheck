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

package modulecheck.parsing.gradle.dsl

import kotlinx.coroutines.runBlocking
import modulecheck.model.dependency.ConfiguredDependency
import modulecheck.model.dependency.ExternalDependency
import modulecheck.model.dependency.ProjectDependency
import modulecheck.utils.prefixIfNot
import modulecheck.utils.replaceDestructured
import modulecheck.utils.suffixIfNot

/**
 * @param configuredDependency the dependency model being added
 * @param newDeclaration the text to be added to the project's build file
 * @param existingMarkerDeclaration if not null, the new declaration will be added above or beyond
 *     this declaration. Of all declarations in the `dependencies { ... }` block, this declaration
 *     should be closest to the desired location of the new declaration.
 * @receiver the project to which we're adding a dependency
 * @since 0.12.0
 */
fun HasDependencyDeclarations.addDependency(
  configuredDependency: ConfiguredDependency,
  newDeclaration: DependencyDeclaration,
  existingMarkerDeclaration: DependencyDeclaration? = null
) {

  if (existingMarkerDeclaration != null) {
    prependStatement(
      newDeclaration = newDeclaration,
      existingDeclaration = existingMarkerDeclaration
    )
  } else {
    addStatement(newDeclaration = newDeclaration)
  }

  when (configuredDependency) {
    is ProjectDependency -> projectDependencies.add(configuredDependency)
    is ExternalDependency -> externalDependencies.add(configuredDependency)
  }
}

private fun HasDependencyDeclarations.prependStatement(
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

private fun HasDependencyDeclarations.addStatement(
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

      val newBlock = "dependencies {\n${newStatement.suffixIfNot("\n")}}"
        .prefixIfNot("\n\n")
      val newText = buildFileText + newBlock

      buildFile.writeText(newText)
    }
  }
}

fun HasDependencyDeclarations.removeDependencyWithComment(
  statement: BuildFileStatement,
  fixLabel: String,
  configuredDependency: ConfiguredDependency? = null
) {
  synchronized(buildFile) {

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
          "$whitespace// $code"
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

    if (configuredDependency is ProjectDependency) {

      projectDependencies.remove(configuredDependency)
    }
  }
}

fun HasDependencyDeclarations.removeDependencyWithDelete(
  statement: BuildFileStatement,
  configuredDependency: ConfiguredDependency? = null
) {
  synchronized(buildFile) {
    val text = buildFile.readText()

    buildFile.writeText(
      // the `prefixIfNot("\n")` here is important.
      // It needs to match what we're doing if we add a new dependency.  Otherwise, we wind up adding
      // or removing newlines instead of just modifying the dependencies.
      // See https://github.com/RBusarow/ModuleCheck/issues/443
      text.replaceFirst(statement.statementWithSurroundingText.prefixIfNot("\n"), "")
    )

    if (configuredDependency is ProjectDependency) {
      projectDependencies.remove(configuredDependency)
    }
  }
}
