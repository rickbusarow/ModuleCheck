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

package modulecheck.api.finding

import modulecheck.parsing.gradle.Declaration
import modulecheck.parsing.gradle.DependencyDeclaration
import modulecheck.project.ConfiguredDependency
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject
import org.jetbrains.kotlin.util.prefixIfNot

fun McProject.addDependency(
  cpd: ConfiguredProjectDependency,
  newDeclaration: DependencyDeclaration,
  markerDeclaration: DependencyDeclaration
) = synchronized(buildFile) {

  val oldStatement = markerDeclaration.statementWithSurroundingText
  val newStatement = newDeclaration.statementWithSurroundingText

  // the `prefixIfNot("\n")` here is important.
  // It needs to match what we're doing if we delete a dependency.  Otherwise, we wind up adding
  // or removing newlines instead of just modifying the dependencies.
  // See https://github.com/RBusarow/ModuleCheck/issues/443
  val combinedStatement = newStatement.plus(oldStatement.prefixIfNot("\n"))

  val buildFileText = buildFile.readText()

  buildFile.writeText(buildFileText.replace(oldStatement, combinedStatement))

  projectDependencies.add(cpd)
}

fun McProject.removeDependencyWithComment(
  declaration: Declaration,
  fixLabel: String,
  configuredDependency: ConfiguredDependency? = null
) = synchronized(buildFile) {

  val text = buildFile.readText()

  val statementText = declaration.statementWithSurroundingText

  val lines = statementText.lines()
  val lastIndex = lines.lastIndex
  val newText = lines
    .mapIndexed { index: Int, str: String ->

      // don't comment out a blank line
      if (str.isBlank()) return@mapIndexed str

      val commented = str.replace("""(\s*)(\S.*)""".toRegex()) { mr ->
        val (whitespace, code) = mr.destructured
        "$whitespace${Fixable.INLINE_COMMENT}$code"
      }

      if (index == lastIndex) {
        commented + fixLabel
      } else {
        commented
      }
    }
    .joinToString("\n")

  buildFile.writeText(text.replaceFirst(statementText, newText))

  if (configuredDependency is ConfiguredProjectDependency) {

    projectDependencies.remove(configuredDependency)
  }
}

fun McProject.removeDependencyWithDelete(
  declaration: Declaration,
  configuredDependency: ConfiguredDependency? = null
) = synchronized(buildFile) {
  val text = buildFile.readText()

  buildFile.writeText(
    // the `prefixIfNot("\n")` here is important.
    // It needs to match what we're doing if we add a new dependency.  Otherwise, we wind up adding
    // or removing newlines instead of just modifying the dependencies.
    // See https://github.com/RBusarow/ModuleCheck/issues/443
    text.replaceFirst(declaration.statementWithSurroundingText.prefixIfNot("\n"), "")
  )

  if (configuredDependency is ConfiguredProjectDependency) {
    projectDependencies.remove(configuredDependency)
  }
}
