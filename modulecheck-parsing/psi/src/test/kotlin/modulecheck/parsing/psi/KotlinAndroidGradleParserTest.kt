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

package modulecheck.parsing.psi

import hermit.test.junit.HermitJUnit5
import io.kotest.matchers.shouldBe
import modulecheck.parsing.gradle.dsl.AndroidGradleSettings
import modulecheck.parsing.gradle.dsl.AndroidGradleSettings.AgpBlock.AndroidBlock
import modulecheck.parsing.gradle.dsl.AndroidGradleSettings.AgpBlock.BuildFeaturesBlock
import modulecheck.parsing.gradle.dsl.Assignment
import modulecheck.reporting.logging.PrintLogger
import modulecheck.testing.tempFile
import org.jetbrains.kotlin.cli.common.repl.replEscapeLineBreaks
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInfo

internal class KotlinAndroidGradleParserTest : HermitJUnit5() {

  val logger by resets { PrintLogger() }

  val testFile by tempFile()

  private var testInfo: TestInfo? = null

  @BeforeEach
  fun beforeEach(testInfo: TestInfo) {
    this.testInfo = testInfo
  }

  @TestFactory
  fun `lots of blocks`() = runTest { enabled ->

    val block = """
      android {
        buildFeatures {
          viewBinding = $enabled
        }
        buildFeatures {
          buildConfig = $enabled
        }
      }
      android {
        buildFeatures {
          androidResources = $enabled
        }
      }
    """.trimIndent()

    testFile.writeText(block)

    val viewBindingAssignment = Assignment(
      fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n  }\n  buildFeatures {\n    buildConfig = $enabled\n  }\n}",
      propertyFullName = "viewBinding",
      value = "$enabled",
      declarationText = "viewBinding = $enabled"
    )
    val buildConfigAssignment = Assignment(
      fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n  }\n  buildFeatures {\n    buildConfig = $enabled\n  }\n}",
      propertyFullName = "buildConfig",
      value = "$enabled",
      declarationText = "buildConfig = $enabled"
    )
    val androidResourcesAssignment = Assignment(
      fullText = "android {\n  buildFeatures {\n    androidResources = $enabled\n  }\n}",
      propertyFullName = "androidResources",
      value = "$enabled",
      declarationText = "androidResources = $enabled"
    )
    KotlinAndroidGradleParser().parse(testFile) shouldBe AndroidGradleSettings(
      assignments = listOf(
        viewBindingAssignment,
        buildConfigAssignment,
        androidResourcesAssignment
      ),
      androidBlocks = listOf(
        AndroidBlock(
          fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n  }\n  buildFeatures {\n    buildConfig = $enabled\n  }\n}",
          lambdaContent = "buildFeatures {\n    viewBinding = $enabled\n  }\n  buildFeatures {\n    buildConfig = $enabled\n  }",
          settings = listOf(
            viewBindingAssignment,
            buildConfigAssignment
          )
        ),
        AndroidBlock(
          fullText = "android {\n  buildFeatures {\n    androidResources = $enabled\n  }\n}",
          lambdaContent = "buildFeatures {\n    androidResources = $enabled\n  }",
          settings = listOf(
            androidResourcesAssignment
          )
        )
      ),
      buildFeaturesBlocks = listOf(
        BuildFeaturesBlock(
          fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n  }\n  buildFeatures {\n    buildConfig = $enabled\n  }\n}",
          lambdaContent = "viewBinding = $enabled",
          settings = listOf(viewBindingAssignment)
        ),
        BuildFeaturesBlock(
          fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n  }\n  buildFeatures {\n    buildConfig = $enabled\n  }\n}",
          lambdaContent = "buildConfig = $enabled",
          settings = listOf(buildConfigAssignment)
        ),
        BuildFeaturesBlock(
          fullText = "android {\n  buildFeatures {\n    androidResources = $enabled\n  }\n}",
          lambdaContent = "androidResources = $enabled",
          settings = listOf(androidResourcesAssignment)
        )
      )
    )
  }

  @TestFactory
  fun `fully scoped boolean property`() = runTest { enabled ->

    val block = """
      android {
        buildFeatures {
          viewBinding = $enabled
          androidResources = ${!enabled}
        }
      }
    """.trimIndent()

    testFile.writeText(block)

    KotlinAndroidGradleParser().parse(testFile) shouldBe AndroidGradleSettings(
      assignments = listOf(
        Assignment(
          fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
          propertyFullName = "viewBinding",
          value = "$enabled",
          declarationText = "viewBinding = $enabled"
        ),
        Assignment(
          fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
          propertyFullName = "androidResources",
          value = "${!enabled}",
          declarationText = "androidResources = ${!enabled}"
        )
      ),
      androidBlocks = listOf(
        AndroidBlock(
          fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
          lambdaContent = "buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }",
          settings = listOf(
            Assignment(
              fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
              propertyFullName = "viewBinding",
              value = "$enabled",
              declarationText = "viewBinding = $enabled"
            ),
            Assignment(
              fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
              propertyFullName = "androidResources",
              value = "${!enabled}",
              declarationText = "androidResources = ${!enabled}"
            )
          )
        )
      ),
      buildFeaturesBlocks = listOf(
        BuildFeaturesBlock(
          fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
          lambdaContent = "viewBinding = $enabled\n    androidResources = ${!enabled}",
          settings = listOf(
            Assignment(
              fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
              propertyFullName = "viewBinding",
              value = "$enabled",
              declarationText = "viewBinding = $enabled"
            ),
            Assignment(
              fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
              propertyFullName = "androidResources",
              value = "${!enabled}",
              declarationText = "androidResources = ${!enabled}"
            )
          )
        )
      )
    )
  }

  @TestFactory
  fun `fully dot qualified boolean property`() = runTest { enabled ->

    val block = """
      android.buildFeatures.androidResources = $enabled
    """.trimIndent()

    testFile.writeText(block)

    KotlinAndroidGradleParser().parse(testFile) shouldBe AndroidGradleSettings(
      assignments = listOf(
        Assignment(
          fullText = "android.buildFeatures.androidResources = $enabled",
          propertyFullName = "androidResources",
          value = "$enabled",
          declarationText = "android.buildFeatures.androidResources = $enabled"
        )
      ),
      androidBlocks = listOf(),
      buildFeaturesBlocks = listOf()
    )
  }

  @TestFactory
  fun `dot qualified and then scoped boolean property`() = runTest { enabled ->

    val block = """
      android.buildFeatures {
        viewBinding = $enabled
      }
    """.trimIndent()

    testFile.writeText(block)

    KotlinAndroidGradleParser().parse(testFile) shouldBe AndroidGradleSettings(
      assignments = listOf(
        Assignment(
          fullText = "android.buildFeatures {\n  viewBinding = $enabled\n}",
          propertyFullName = "viewBinding",
          value = "$enabled",
          declarationText = "viewBinding = $enabled"
        )
      ),
      androidBlocks = listOf(),
      buildFeaturesBlocks = listOf(
        BuildFeaturesBlock(
          fullText = "android.buildFeatures {\n  viewBinding = $enabled\n}",
          lambdaContent = "viewBinding = $enabled",
          settings = listOf(
            Assignment(
              fullText = "android.buildFeatures {\n  viewBinding = $enabled\n}",
              propertyFullName = "viewBinding",
              value = "$enabled",
              declarationText = "viewBinding = $enabled"
            )
          )
        )
      )
    )
  }

  @TestFactory
  fun `scoped and then dot qualified boolean property`() = runTest { enabled ->

    val block = """
      android {
        buildFeatures.viewBinding = $enabled
      }
    """.trimIndent()

    testFile.writeText(block)

    KotlinAndroidGradleParser().parse(testFile) shouldBe AndroidGradleSettings(
      assignments = listOf(
        Assignment(
          fullText = "android {\n  buildFeatures.viewBinding = $enabled\n}",
          propertyFullName = "viewBinding",
          value = "$enabled",
          declarationText = "buildFeatures.viewBinding = $enabled"
        )
      ),
      androidBlocks = listOf(
        AndroidBlock(
          fullText = "android {\n  buildFeatures.viewBinding = $enabled\n}",
          lambdaContent = "buildFeatures.viewBinding = $enabled",
          settings = listOf(
            Assignment(
              fullText = "android {\n  buildFeatures.viewBinding = $enabled\n}",
              propertyFullName = "viewBinding",
              value = "$enabled",
              declarationText = "buildFeatures.viewBinding = $enabled"
            )
          )
        )
      ),
      buildFeaturesBlocks = listOf()
    )
  }

  fun runTest(block: (enabled: Boolean) -> Unit): List<DynamicTest> {
    return listOf(true, false).map { enabled ->

      val paramsString = " -- enabled: $enabled"

      val name = (
        testInfo?.displayName?.replace("()", "")
          ?: "???"
        ) + paramsString

      DynamicTest.dynamicTest(name) {
        block(enabled)
        resetAll()

        System.gc()
      }
    }
  }
}

fun AndroidGradleSettings.buildSettings() = """
        AndroidGradleSettings(
          assignments = ${assignments.buildAssignments()},
          androidBlocks = ${androidBlocks.buildAndroidBlock()},
          buildFeaturesBlocks =${buildFeaturesBlocks.buildBuildFeaturesBlock()}
        )
""".trimIndent()

fun List<BuildFeaturesBlock>.buildBuildFeaturesBlock() =
  joinToString(prefix = "listOf(\n", postfix = "\n)") { block ->
    """BuildFeaturesBlock(
        fullText = "${block.fullText.replEscapeLineBreaks()}",
        lambdaContent = "${block.lambdaContent.replEscapeLineBreaks()}",
        settings = ${block.settings.buildAssignments()}
      )
    """.trimIndent()
  }

fun List<AndroidBlock>.buildAndroidBlock() =
  joinToString(prefix = "listOf(\n", postfix = "\n)") { block ->
    """AndroidBlock(
        fullText = "${block.fullText.replEscapeLineBreaks()}",
        lambdaContent = "${block.lambdaContent.replEscapeLineBreaks()}",
        settings = ${block.settings.buildAssignments()}
      )
    """.trimIndent()
  }

fun List<Assignment>.buildAssignments() =
  joinToString(prefix = "listOf(\n", postfix = "\n)") { assignment ->
    """Assignment(
        fullText = "${assignment.fullText.replEscapeLineBreaks()}",
        propertyFullName = "${assignment.propertyFullName.replEscapeLineBreaks()}",
        value = "${assignment.value.replEscapeLineBreaks()}",
        assignmentText = "${assignment.declarationText.replEscapeLineBreaks()}"
      )
    """.trimIndent()
  }
