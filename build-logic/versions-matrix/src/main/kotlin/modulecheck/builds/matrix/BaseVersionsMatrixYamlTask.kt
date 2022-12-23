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

package modulecheck.builds.matrix

import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationTask
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import java.io.File
import javax.inject.Inject

abstract class BaseVersionsMatrixYamlTask @Inject constructor(
  objectFactory: ObjectFactory
) : DefaultTask() {

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  val yamlFile = objectFactory.fileProperty()

  @get:Internal
  protected val matrixSectionRegex = Regex(
    """( *)(.*$START_TAG.*\n)([\s\S]+?)(.*$END_TAG)"""
  )

  protected fun createYaml(indentSize: Int): String {
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

  protected fun getYamlSections(ciText: String) = matrixSectionRegex
    .findAll(ciText)
    .also { matches ->

      if (!matches.iterator().hasNext()) {
        val message =
          "Couldn't find any `### <start-matrix>`/`### <end-matrix>` sections in the CI file:" +
            "\tfile://${yamlFile.get()}\n\n" +
            "\tSurround the matrix section with the comments '$START_TAG' and `$END_TAG':\n\n" +
            "\t    strategy:\n" +
            "\t      ### $START_TAG\n" +
            "\t      matrix:\n" +
            "\t        [ ... ]\n" +
            "\t      ### $END_TAG\n"

        createStyledOutput()
          .withStyle(StyledTextOutput.Style.Description)
          .println(message)

        require(false)
      }
    }

  protected fun createStyledOutput(): StyledTextOutput = services
    .get(StyledTextOutputFactory::class.java)
    .create("mcbuild-versions-matrix")

  protected fun requireCiFile(): File {
    val ciFile = yamlFile.get().asFile

    require(ciFile.exists()) {
      "Could not resolve file: file://$ciFile"
    }

    return ciFile
  }

  companion object {

    private const val START_TAG = "<start-matrix>"
    private const val END_TAG = "<end-matrix>"
  }
}

abstract class VersionsMatrixYamlCheckTask @Inject constructor(
  objectFactory: ObjectFactory
) : BaseVersionsMatrixYamlTask(objectFactory), VerificationTask {

  @TaskAction
  fun check() {
    val changed = getYamlSections(requireCiFile().readText())
      .map { it.destructured }
      .filter { (indent, _, content, _) ->

        content != createYaml(indent.length)
      }
      .toList()

    if (changed.isNotEmpty()) {
      val message = "The versions matrix in the CI file is out of date.  " +
        "Run ./gradlew versionsMatrixGenerateYaml to automatically update." +
        "\n\tfile://${yamlFile.get()}"

      createStyledOutput()
        .withStyle(StyledTextOutput.Style.Description)
        .println(message)

      require(false)
    }
  }
}

abstract class VersionsMatrixYamlGenerateTask @Inject constructor(
  objectFactory: ObjectFactory
) : BaseVersionsMatrixYamlTask(objectFactory) {

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
    }
  }
}
