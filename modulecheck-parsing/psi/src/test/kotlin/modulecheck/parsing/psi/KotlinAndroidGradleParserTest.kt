/*
 * Copyright (C) 2021-2024 Rick Busarow
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

import com.rickbusarow.kase.DefaultTestEnvironment
import com.rickbusarow.kase.DefaultTestEnvironment.Factory
import com.rickbusarow.kase.Kase1
import com.rickbusarow.kase.KaseTestFactory
import com.rickbusarow.kase.files.HasWorkingDir
import com.rickbusarow.kase.kases
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import modulecheck.parsing.gradle.dsl.AndroidGradleSettings
import modulecheck.parsing.gradle.dsl.AndroidGradleSettings.AgpBlock.AndroidBlock
import modulecheck.parsing.gradle.dsl.AndroidGradleSettings.AgpBlock.BuildFeaturesBlock
import modulecheck.parsing.gradle.dsl.Assignment
import modulecheck.parsing.kotlin.compiler.NoContextPsiFileFactory
import modulecheck.utils.createSafely
import modulecheck.utils.resolve
import org.jetbrains.kotlin.cli.common.repl.replEscapeLineBreaks
import org.junit.jupiter.api.TestFactory
import java.io.File

internal class KotlinAndroidGradleParserTest :
  KaseTestFactory<Kase1<Boolean>, com.rickbusarow.kase.TestEnvironment, Factory> {

  override val testEnvironmentFactory = DefaultTestEnvironment.Factory()

  override val params = kases(listOf(true, false), displayNameFactory = { "enabled: $a1" })

  val HasWorkingDir.testFile: File
    get() = workingDir.resolve("build.gradle.kts").createSafely()

  @TestFactory
  fun `lots of blocks`() = testFactory { (enabled) ->

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
      fullText = """
        android {
          buildFeatures {
            viewBinding = $enabled
          }
          buildFeatures {
            buildConfig = $enabled
          }
        }
      """.trimIndent(),
      propertyFullName = "viewBinding",
      value = "$enabled",
      declarationText = "viewBinding = $enabled",
      statementWithSurroundingText = "viewBinding = $enabled",
      suppressed = listOf("disable-android-buildConfig")
    )
    val buildConfigAssignment = Assignment(
      fullText = """
        android {
          buildFeatures {
            viewBinding = $enabled
          }
          buildFeatures {
            buildConfig = $enabled
          }
        }
      """.trimIndent(),
      propertyFullName = "buildConfig",
      value = "$enabled",
      declarationText = "buildConfig = $enabled",
      statementWithSurroundingText = "buildConfig = $enabled",
      suppressed = listOf("disable-android-buildConfig")
    )
    val declarationText1 = "androidResources = $enabled"
    val androidResourcesAssignment = Assignment(
      fullText = """
        android {
          buildFeatures {
            androidResources = $enabled
          }
        }
      """.trimIndent(),
      propertyFullName = "androidResources",
      value = "$enabled",
      declarationText = declarationText1,
      statementWithSurroundingText = declarationText1,
      suppressed = emptyList()
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
        blockSuppressed = emptyList()
      )
    )

    result.buildFeaturesBlocks shouldContainExactlyInAnyOrder listOf(
      BuildFeaturesBlock(
        fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n  }\n  buildFeatures {\n    buildConfig = $enabled\n  }\n}",
        lambdaContent = "viewBinding = $enabled",
        settings = listOf(viewBindingAssignment),
        blockSuppressed = emptyList()
      ),
      BuildFeaturesBlock(
        fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n  }\n  buildFeatures {\n    buildConfig = $enabled\n  }\n}",
        lambdaContent = "buildConfig = $enabled",
        settings = listOf(buildConfigAssignment),
        blockSuppressed = emptyList()
      ),
      BuildFeaturesBlock(
        fullText = "android {\n  buildFeatures {\n    androidResources = $enabled\n  }\n}",
        lambdaContent = "androidResources = $enabled",
        settings = listOf(androidResourcesAssignment),
        blockSuppressed = emptyList()
      )
    )
  }

  @TestFactory
  fun `fully scoped boolean property`() = testFactory { (enabled) ->

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
            suppressed = emptyList()
          ),
          Assignment(
            fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
            propertyFullName = "androidResources",
            value = "${!enabled}",
            declarationText = declarationText2,
            statementWithSurroundingText = declarationText2,
            suppressed = emptyList()
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
                suppressed = emptyList()
              ),
              Assignment(
                fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
                propertyFullName = "androidResources",
                value = "${!enabled}",
                declarationText = declarationText4,
                statementWithSurroundingText = declarationText4,
                suppressed = emptyList()
              )
            ),
            blockSuppressed = emptyList()
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
                suppressed = emptyList()
              ),
              Assignment(
                fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
                propertyFullName = "androidResources",
                value = "${!enabled}",
                declarationText = declarationText6,
                statementWithSurroundingText = declarationText6,
                suppressed = emptyList()
              )
            ),
            blockSuppressed = emptyList()
          )
        )
      )
    }
  }

  @TestFactory
  fun `fully dot qualified boolean property`() = testFactory { (enabled) ->

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
      androidBlocks = emptyList(),
      buildFeaturesBlocks = emptyList()
    )
  }

  @TestFactory
  fun `dot qualified and then scoped boolean property`() = testFactory { (enabled) ->

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
            suppressed = emptyList()
          )
        ),
        androidBlocks = emptyList(),
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
                suppressed = emptyList()
              )
            ),
            blockSuppressed = emptyList()
          )
        )
      )
    }
  }

  @TestFactory
  fun `scoped and then dot qualified boolean property`() = testFactory { (enabled) ->

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
          blockSuppressed = emptyList()
        )
      ),
      buildFeaturesBlocks = emptyList()
    )
  }

  fun parse(file: File) = runBlocking {
    KotlinAndroidGradleParser(NoContextPsiFileFactory()).parse(file)
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
