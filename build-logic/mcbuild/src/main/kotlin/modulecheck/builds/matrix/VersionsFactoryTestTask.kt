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

import modulecheck.builds.getOrNullFinal
import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.property
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.konan.file.File
import javax.inject.Inject

abstract class VersionsFactoryTestTask @Inject constructor(
  objectFactory: ObjectFactory
) : DefaultTask() {

  @get:Input
  @get:Optional
  @Option(option = "gradleVersion", description = "Gradle version")
  val gradleVersion = objectFactory.property<String?>()

  @get:Input
  @get:Optional
  @Option(option = "agpVersion", description = "AGP version")
  val agpVersion = objectFactory.property<String?>()

  @get:Input
  @get:Optional
  @Option(option = "anvilVersion", description = "Anvil version")
  val anvilVersion = objectFactory.property<String?>()

  @get:Input
  @get:Optional
  @Option(option = "kotlinVersion", description = "Kotlin version")
  val kotlinVersion = objectFactory.property<String?>()

  @get:Input
  @get:Optional
  @Option(option = "exhaustive", description = "exhaustive")
  val exhaustive = objectFactory.property<Boolean?>()

  @get:Input
  val packageName = objectFactory.property<String>()

  @get:OutputDirectory
  val outDir = objectFactory.fileProperty()

  @TaskAction
  fun execute() {

    val packageNameString = packageName.get()

    outDir.get().asFile.deleteRecursively()

    val versionsMatrix = VersionsMatrix(
      exhaustive = exhaustive.getOrNullFinal() ?: false,
      gradleArg = gradleVersion.orNull?.noInt(),
      agpArg = agpVersion.orNull?.noInt(),
      anvilArg = anvilVersion.orNull?.noInt(),
      kotlinArg = kotlinVersion.orNull?.noInt()
    )

    val fullList = versionsMatrix.allValid
      .joinToString(
        separator = ",\n", prefix = "listOf(\n", postfix = "\n|      )"
      ) { (gradle, agp, anvil, kotlin) ->
        "|        TestVersions(" +
          """gradle = "$gradle", """ +
          """agp = "$agp", """ +
          """anvil = "$anvil", """ +
          """kotlin = "$kotlin"""" +
          ")"
      }

    @Language("kotlin")
    val content = """
      |// ktlint-disable
      |@file:Suppress(
      |  "AbsentOrWrongFileLicense",
      |  "MaxLineLength",
      |  "UndocumentedPublicClass",
      |  "UndocumentedPublicFunction",
      |  "UndocumentedPublicProperty"
      |)
      |
      |package $packageNameString
      |
      |interface VersionsFactory {
      |
      |  val exhaustive: Boolean
      |    get() = ${versionsMatrix.exhaustive}
      |
      |  fun versions(
      |    exhaustive: Boolean = this.exhaustive
      |  ): List<TestVersions> {
      |    return if (!exhaustive) {
      |      listOf(nonExhaustiveDefaults())
      |    } else {
      |      $fullList
      |    }
      |  }
      |}
      |
      |internal fun nonExhaustiveDefaults(): TestVersions =
      |  TestVersions(
      |    gradle = "${versionsMatrix.defaultGradle}",
      |    agp = "${versionsMatrix.defaultAgp}",
      |    anvil = "${versionsMatrix.defaultAnvil}",
      |    kotlin = "${versionsMatrix.defaultKotlin}"
      |  )
    """.trimMargin()

    val packageDir = packageNameString.replace(".", File.separator)

    val generatedFile = outDir.get().asFile
      .resolve(packageDir)
      .also { it.mkdirs() }
      .resolve("VersionsFactory.kt")

    generatedFile.writeText(content)
  }

  /**
   * The GitHub Actions test matrix parses "7.0" into an integer and passes in a command line
   * argument of "7". That version doesn't resolve. So if the string doesn't contain a period, just
   * append ".0"
   *
   * @since 0.13.0
   */
  private fun String.noInt() = if (!contains(".")) {
    "$this.0"
  } else {
    this
  }
}
