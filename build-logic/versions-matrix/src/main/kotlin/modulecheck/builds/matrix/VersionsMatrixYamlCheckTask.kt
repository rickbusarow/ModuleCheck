/*
 * Copyright (C) 2021-2023 Rick Busarow
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

package modulecheck.builds.matrix

import modulecheck.builds.BaseYamlMatrixTask
import modulecheck.builds.diffString
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationTask
import org.gradle.internal.logging.text.StyledTextOutput
import javax.inject.Inject

abstract class VersionsMatrixYamlCheckTask @Inject constructor(
  objectFactory: ObjectFactory
) : BaseYamlMatrixTask(objectFactory), VerificationTask {

  @TaskAction
  fun check() {
    val ciFile = requireCiFile()

    val ciText = ciFile.readText()
    val pattern = matrixSectionRegex

    val newText = ciText.replace(pattern) { match ->

      val (indent, startTag, _, closingLine) = match.destructured

      val newContent = createYaml(indent.length)

      "$indent$startTag$newContent$closingLine"
    }

    if (ciText != newText) {
      val message = "The versions matrix in the CI file is out of date.  " +
        "Run ./gradlew versionsMatrixGenerateYaml to automatically update." +
        "\n\tfile://${yamlFile.get()}"

      createStyledOutput()
        .withStyle(StyledTextOutput.Style.Description)
        .println(message)

      println()
      println(diffString(ciText, newText))
      println()

      require(false)
    }
  }
}

abstract class VersionsMatrixYamlGenerateTask @Inject constructor(
  objectFactory: ObjectFactory
) : BaseYamlMatrixTask(objectFactory) {

  @TaskAction
  fun execute() {
    val ciFile = requireCiFile()

    val ciText = ciFile.readText()
    val pattern = matrixSectionRegex

    val newText = ciText.replace(pattern) { match ->

      val (indent, startTag, _, closingLine) = match.destructured

      val newContent = createYaml(indent.length)

      "$indent$startTag$newContent$closingLine"
    }

    if (ciText != newText) {

      ciFile.writeText(newText)

      val message = "Updated the versions matrix in the CI file." +
        "\n\tfile://${yamlFile.get()}"

      createStyledOutput()
        .withStyle(StyledTextOutput.Style.Description)
        .println(message)

      println()
      println(diffString(ciText, newText))
      println()
    }
  }
}

private fun createYaml(indentSize: Int): String {
  val versionsMatrix = VersionsMatrix(
    exhaustive = true,
    gradleArg = null,
    agpArg = null,
    anvilArg = null,
    kotlinArg = null
  )

  return VersionsMatrixYamlGenerator()
    .generate(
      versionsMatrix = versionsMatrix,
      indentSize = indentSize
    )
}
