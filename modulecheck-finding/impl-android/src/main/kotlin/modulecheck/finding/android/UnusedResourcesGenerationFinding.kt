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

package modulecheck.finding.android

import modulecheck.finding.Finding
import modulecheck.finding.Finding.Position
import modulecheck.finding.FindingName
import modulecheck.finding.Fixable
import modulecheck.finding.RemovesDependency.RemovalStrategy
import modulecheck.finding.internal.positionOfStatement
import modulecheck.parsing.gradle.dsl.AndroidGradleSettings.AgpBlock
import modulecheck.parsing.gradle.dsl.BuildFileStatement
import modulecheck.parsing.gradle.model.ProjectPath
import modulecheck.project.McProject
import modulecheck.utils.findMinimumIndent
import modulecheck.utils.indent
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.lazyDeferred
import org.jetbrains.kotlin.util.suffixIfNot
import java.io.File

data class UnusedResourcesGenerationFinding(
  override val dependentProject: McProject,
  override val dependentPath: ProjectPath.StringProjectPath,
  override val buildFile: File
) : Finding, Fixable {

  override val findingName = NAME

  override val message: String
    get() = "`androidResources` generation is enabled, but no resources are defined in this module."

  override val dependencyIdentifier = ""

  override val statementOrNull: LazyDeferred<BuildFileStatement?> = lazyDeferred {
    dependentProject.buildFileParser.androidSettings()
      .assignments
      .firstOrNull { it.propertyFullName == "androidResources" }
  }

  override val statementTextOrNull = lazyDeferred {
    statementOrNull.await()?.declarationText
  }

  override val positionOrNull: LazyDeferred<Position?> = lazyDeferred {
    val statement = statementTextOrNull.await() ?: return@lazyDeferred null

    val fileText = buildFile.readText()

    fileText.positionOfStatement(statement)
  }

  override suspend fun fix(removalStrategy: RemovalStrategy): Boolean {

    val settings = dependentProject.buildFileParser.androidSettings()

    val newText = settings.assignments
      .filter { it.propertyFullName == "androidResources" && it.value == "true" }
      .takeIf { it.isNotEmpty() }
      ?.fold(buildFile.readText()) { oldText, assignment ->

        val newAssignmentText = assignment.declarationText.replace(assignment.value, "false")

        val newFullText = assignment.fullText
          .replace(assignment.declarationText, newAssignmentText)

        oldText.replace(assignment.fullText, newFullText)
      }
      ?: settings.buildFeaturesBlocks.firstOrNull()
        ?.withAddedStatement("androidResources = false")
      ?: settings.androidBlocks.firstOrNull()
        ?.withAddedStatement("buildFeatures.androidResources = false")
      ?: newAndroidBlock()

    buildFile.writeText(newText)

    return true
  }

  private suspend fun newAndroidBlock(): String {

    val indent = buildFile.findMinimumIndent()

    val androidBlock = buildString {
      appendLine("android {")
      indent(indent) {
        appendLine("buildFeatures {")

        indent(indent) {
          appendLine("androidResources = false")
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
          .removeSuffix("\n")

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

  private fun AgpBlock.withAddedStatement(newStatement: String): String {

    val indent = lambdaContent.findMinimumIndent()

    val newContent = lambdaContent.plus("\n$indent$newStatement")

    val newBlockText = fullText.replace(lambdaContent, newContent)

    return buildFile.readText().replace(fullText, newBlockText)
  }

  companion object {
    /** @suppress */
    val NAME = FindingName("disable-android-resources")
  }
}
