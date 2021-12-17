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

package modulecheck.core.rule.android

import modulecheck.api.finding.Finding
import modulecheck.api.finding.Finding.Position
import modulecheck.api.finding.Fixable
import modulecheck.core.internal.positionOfStatement
import modulecheck.parsing.gradle.Block
import modulecheck.project.McProject
import modulecheck.utils.indent
import modulecheck.utils.minimumIndent
import org.jetbrains.kotlin.util.suffixIfNot
import java.io.File

data class DisableViewBindingGenerationFinding(
  override val dependentProject: McProject,
  override val dependentPath: String,
  override val buildFile: File
) : Finding, Fixable {

  override val message: String
    get() = "Android viewBinding generation is enabled, but no generated code is being used."

  override val findingName = "disableViewBinding"

  override val dependencyIdentifier = ""

  override val statementTextOrNull: String? by lazy {

    dependentProject.buildFileParser.androidSettings()
      .assignments
      .firstOrNull { it.propertyFullName == "viewBinding" }
      ?.assignmentText
  }

  override val positionOrNull: Position? by lazy {
    val statement = statementTextOrNull ?: return@lazy null

    val fileText = buildFile.readText()

    fileText.positionOfStatement(statement)
  }

  override fun fix(): Boolean = synchronized(buildFile) {

    val settings = dependentProject.buildFileParser.androidSettings()

    val newText = settings.assignments
      .filter { it.propertyFullName == "viewBinding" && it.value == "true" }
      .takeIf { it.isNotEmpty() }
      ?.fold(buildFile.readText()) { oldText, assignment ->

        val newAssignmentText = assignment.assignmentText.replace(assignment.value, "false")

        val newFullText = assignment.fullText
          .replace(assignment.assignmentText, newAssignmentText)

        oldText.replace(assignment.fullText, newFullText)
      }
      ?: settings.buildFeaturesBlocks.firstOrNull()
        ?.withAddedStatement("viewBinding = false")
      ?: settings.androidBlocks.firstOrNull()
        ?.withAddedStatement("buildFeatures.viewBinding = false")
      ?: newAndroidBlock()

    buildFile.writeText(newText)

    return true
  }

  private fun newAndroidBlock(): String {

    val indent = buildFile.minimumIndent()

    val androidBlock = buildString {
      appendLine("android {")
      indent(indent) {
        appendLine("buildFeatures {")

        indent(indent) {
          appendLine("viewBinding = false")
        }
        appendLine('}')
      }
      appendLine('}')
    }

    val oldText = buildFile.readText()

    return dependentProject.buildFileParser
      .pluginsBlock()
      ?.fullText
      ?.let { oldPlugins ->

        val new = "$oldPlugins\n\n$androidBlock"

        oldText.replace(oldPlugins, new)
      }
      ?: dependentProject.buildFileParser
        .dependenciesBlocks()
        .firstOrNull()
        ?.fullText
        ?.let { oldDependencies ->

          val new = "$androidBlock\n$oldDependencies"
          oldText.replace(oldDependencies, new)
        }
      ?: (oldText.suffixIfNot("\n") + "\n$androidBlock")
  }

  private fun Block.withAddedStatement(newStatement: String): String {

    val indent = lambdaContent.minimumIndent()

    val newContent = lambdaContent.plus("\n$indent$newStatement")

    val newBlockText = fullText.replace(lambdaContent, newContent)

    return buildFile.readText().replace(fullText, newBlockText)
  }
}
