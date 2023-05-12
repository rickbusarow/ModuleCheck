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

package modulecheck.builds

import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutput.Style.Description
import org.gradle.internal.logging.text.StyledTextOutputFactory
import java.io.File
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE

abstract class BaseYamlMatrixTask @Inject constructor(
  objectFactory: ObjectFactory
) : DefaultTask() {

  @get:InputFile
  @get:PathSensitive(RELATIVE)
  val yamlFile = objectFactory.fileProperty()

  @get:Input abstract val startTagProperty: Property<String>
  private val startTag: String
    get() = startTagProperty.getFinal()

  @get:Input abstract val endTagProperty: Property<String>
  private val endTag: String
    get() = endTagProperty.getFinal()

  @get:Internal
  protected val matrixSectionRegex by lazy(NONE) {

    val startTagEscaped = Regex.escape(startTag)
    val endTagEscaped = Regex.escape(endTag)

    Regex("""( *)(.*$startTagEscaped.*\n)([\s\S]+?)(.*$endTagEscaped)""")
  }

  protected fun getYamlSections(ciText: String) = matrixSectionRegex
    .findAll(ciText)
    .also { matches ->

      if (!matches.iterator().hasNext()) {
        val message =
          "Couldn't find any `$startTag`/`$endTag` sections in the CI file:" +
            "\tfile://${yamlFile.get()}\n\n" +
            "\tSurround the matrix section with the comments '$startTag' and `$endTag':\n\n" +
            "\t    strategy:\n" +
            "\t      ### $startTag\n" +
            "\t      matrix:\n" +
            "\t        [ ... ]\n" +
            "\t      ### $endTag\n"

        createStyledOutput()
          .withStyle(Description)
          .println(message)

        require(false)
      }
    }

  protected fun createStyledOutput(): StyledTextOutput = services
    .get(StyledTextOutputFactory::class.java)
    .create("mcbuild-yaml-matrix")

  protected fun requireCiFile(): File {
    val ciFile = yamlFile.get().asFile

    require(ciFile.exists()) {
      "Could not resolve file: file://$ciFile"
    }

    return ciFile
  }
}
