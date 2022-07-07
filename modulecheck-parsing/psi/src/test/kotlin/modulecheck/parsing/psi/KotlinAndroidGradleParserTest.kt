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

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlinx.coroutines.runBlocking
import modulecheck.parsing.gradle.dsl.AndroidGradleSettings
import modulecheck.parsing.gradle.dsl.AndroidGradleSettings.AgpBlock.AndroidBlock
import modulecheck.parsing.gradle.dsl.AndroidGradleSettings.AgpBlock.BuildFeaturesBlock
import modulecheck.parsing.gradle.dsl.Assignment
import modulecheck.parsing.kotlin.compiler.NoContextPsiFileFactory
import modulecheck.testing.BaseTest
import modulecheck.utils.child
import modulecheck.utils.createSafely
import org.jetbrains.kotlin.cli.common.repl.replEscapeLineBreaks
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File

internal class KotlinAndroidGradleParserTest : BaseTest() {

  val testFile by resets {
    testProjectDir.child("build.gradle.kts").createSafely()
  }

  @TestFactory
  fun `lots of blocks`() = runTest { enabled ->

    val block = """
      @Suppress("disable-android-buildConfig")
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
      declarationText = "viewBinding = $enabled",
      statementWithSurroundingText = "viewBinding = $enabled",
      suppressed = listOf("disable-android-buildConfig")
    )
    val buildConfigAssignment = Assignment(
      fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n  }\n  buildFeatures {\n    buildConfig = $enabled\n  }\n}",
      propertyFullName = "buildConfig",
      value = "$enabled",
      declarationText = "buildConfig = $enabled",
      statementWithSurroundingText = "buildConfig = $enabled",
      suppressed = listOf("disable-android-buildConfig")
    )
    val declarationText1 = "androidResources = $enabled"
    val androidResourcesAssignment = Assignment(
      fullText = "android {\n  buildFeatures {\n    androidResources = $enabled\n  }\n}",
      propertyFullName = "androidResources",
      value = "$enabled",
      declarationText = declarationText1,
      statementWithSurroundingText = declarationText1,
      suppressed = listOf()
    )
    val result = parse(testFile)

    result.assignments shouldContainExactlyInAnyOrder listOf(
      viewBindingAssignment,
      buildConfigAssignment,
      androidResourcesAssignment
    )

    result.androidBlocks shouldContainExactlyInAnyOrder listOf(
      AndroidBlock(
        fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n  }\n  buildFeatures {\n    buildConfig = $enabled\n  }\n}",
        lambdaContent = "buildFeatures {\n    viewBinding = $enabled\n  }\n  buildFeatures {\n    buildConfig = $enabled\n  }",
        settings = listOf(
          viewBindingAssignment,
          buildConfigAssignment
        ),
        blockSuppressed = listOf("disable-android-buildConfig")
      ),
      AndroidBlock(
        fullText = "android {\n  buildFeatures {\n    androidResources = $enabled\n  }\n}",
        lambdaContent = "buildFeatures {\n    androidResources = $enabled\n  }",
        settings = listOf(
          androidResourcesAssignment
        ),
        blockSuppressed = listOf()
      )
    )

    result.buildFeaturesBlocks shouldContainExactlyInAnyOrder listOf(
      BuildFeaturesBlock(
        fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n  }\n  buildFeatures {\n    buildConfig = $enabled\n  }\n}",
        lambdaContent = "viewBinding = $enabled",
        settings = listOf(viewBindingAssignment),
        blockSuppressed = listOf()
      ),
      BuildFeaturesBlock(
        fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n  }\n  buildFeatures {\n    buildConfig = $enabled\n  }\n}",
        lambdaContent = "buildConfig = $enabled",
        settings = listOf(buildConfigAssignment),
        blockSuppressed = listOf()
      ),
      BuildFeaturesBlock(
        fullText = "android {\n  buildFeatures {\n    androidResources = $enabled\n  }\n}",
        lambdaContent = "androidResources = $enabled",
        settings = listOf(androidResourcesAssignment),
        blockSuppressed = listOf()
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

    parse(testFile) shouldBe run {
      val declarationText1 = "viewBinding = $enabled"
      val declarationText2 = "androidResources = ${!enabled}"
      val declarationText3 = "viewBinding = $enabled"
      val declarationText4 = "androidResources = ${!enabled}"
      val declarationText5 = "viewBinding = $enabled"
      val declarationText6 = "androidResources = ${!enabled}"
      AndroidGradleSettings(
        assignments = listOf(
          Assignment(
            fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
            propertyFullName = "viewBinding",
            value = "$enabled",
            declarationText = declarationText1,
            statementWithSurroundingText = declarationText1,
            suppressed = listOf()
          ),
          Assignment(
            fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
            propertyFullName = "androidResources",
            value = "${!enabled}",
            declarationText = declarationText2,
            statementWithSurroundingText = declarationText2,
            suppressed = listOf()
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
                declarationText = declarationText3,
                statementWithSurroundingText = declarationText3,
                suppressed = listOf()
              ),
              Assignment(
                fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
                propertyFullName = "androidResources",
                value = "${!enabled}",
                declarationText = declarationText4,
                statementWithSurroundingText = declarationText4,
                suppressed = listOf()
              )
            ),
            blockSuppressed = listOf()
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
                declarationText = declarationText5,
                statementWithSurroundingText = declarationText5,
                suppressed = listOf()
              ),
              Assignment(
                fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
                propertyFullName = "androidResources",
                value = "${!enabled}",
                declarationText = declarationText6,
                statementWithSurroundingText = declarationText6,
                suppressed = listOf()
              )
            ),
            blockSuppressed = listOf()
          )
        )
      )
    }
  }

  @TestFactory
  fun `fully dot qualified boolean property`() = runTest { enabled ->

    val block = """
      @Suppress("disable-android-resources")
      android.buildFeatures.androidResources = $enabled
    """.trimIndent()

    testFile.writeText(block)

    parse(testFile) shouldBe AndroidGradleSettings(
      assignments = listOf(
        Assignment(
          fullText = "android.buildFeatures.androidResources = $enabled",
          propertyFullName = "androidResources",
          value = "$enabled",
          declarationText = "android.buildFeatures.androidResources = $enabled",
          statementWithSurroundingText = "android.buildFeatures.androidResources = $enabled",
          suppressed = listOf("disable-android-resources")
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

    parse(testFile) shouldBe run {
      val declarationText1 = "viewBinding = $enabled"
      val declarationText2 = "viewBinding = $enabled"
      AndroidGradleSettings(
        assignments = listOf(
          Assignment(
            fullText = "android.buildFeatures {\n  viewBinding = $enabled\n}",
            propertyFullName = "viewBinding",
            value = "$enabled",
            declarationText = declarationText1,
            statementWithSurroundingText = declarationText1,
            suppressed = listOf()
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
                declarationText = declarationText2,
                statementWithSurroundingText = declarationText2,
                suppressed = listOf()
              )
            ),
            blockSuppressed = listOf()
          )
        )
      )
    }
  }

  @TestFactory
  fun `scoped and then dot qualified boolean property`() = runTest { enabled ->

    val block = """
      android {
        @Suppress("disable-view-binding")
        buildFeatures.viewBinding = $enabled
      }
    """.trimIndent()

    testFile.writeText(block)

    parse(testFile) shouldBe AndroidGradleSettings(
      assignments = listOf(
        Assignment(
          fullText = "android {\n  @Suppress(\"disable-view-binding\")\n  buildFeatures.viewBinding = $enabled\n}",
          propertyFullName = "viewBinding",
          value = "$enabled",
          declarationText = "buildFeatures.viewBinding = $enabled",
          statementWithSurroundingText = "buildFeatures.viewBinding = $enabled",
          suppressed = listOf("disable-view-binding")
        )
      ),
      androidBlocks = listOf(
        AndroidBlock(
          fullText = "android {\n  @Suppress(\"disable-view-binding\")\n  buildFeatures.viewBinding = $enabled\n}",
          lambdaContent = "@Suppress(\"disable-view-binding\")\n  buildFeatures.viewBinding = $enabled",
          settings = listOf(
            Assignment(
              fullText = "android {\n  @Suppress(\"disable-view-binding\")\n  buildFeatures.viewBinding = $enabled\n}",
              propertyFullName = "viewBinding",
              value = "$enabled",
              declarationText = "buildFeatures.viewBinding = $enabled",
              statementWithSurroundingText = "buildFeatures.viewBinding = $enabled",
              suppressed = listOf("disable-view-binding")
            )
          ),
          blockSuppressed = listOf()
        )
      ),
      buildFeaturesBlocks = listOf()
    )
  }

  fun parse(file: File) = runBlocking {
    KotlinAndroidGradleParser(NoContextPsiFileFactory()).parse(file)
  }

  fun runTest(block: (enabled: Boolean) -> Unit): List<DynamicTest> {
    return listOf(true, false).map { enabled ->

      val paramsString = " -- enabled: $enabled"

      val name = "${testDisplayName.replace("()", "")}$paramsString"

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
