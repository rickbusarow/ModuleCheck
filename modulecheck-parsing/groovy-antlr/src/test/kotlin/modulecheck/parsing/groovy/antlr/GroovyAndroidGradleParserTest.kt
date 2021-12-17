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

package modulecheck.parsing.groovy.antlr

import hermit.test.junit.HermitJUnit5
import io.kotest.matchers.shouldBe
import modulecheck.parsing.gradle.AndroidGradleSettings
import modulecheck.parsing.gradle.Assignment
import modulecheck.parsing.gradle.Block.AndroidBlock
import modulecheck.parsing.gradle.Block.BuildFeaturesBlock
import modulecheck.testing.tempFile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInfo

internal class GroovyAndroidGradleParserTest : HermitJUnit5() {

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
      assignmentText = "viewBinding = $enabled"
    )
    val buildConfigAssignment = Assignment(
      fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n  }\n  buildFeatures {\n    buildConfig = $enabled\n  }\n}",
      propertyFullName = "buildConfig",
      value = "$enabled",
      assignmentText = "buildConfig = $enabled"
    )
    val androidResourcesAssignment = Assignment(
      fullText = "android {\n  buildFeatures {\n    androidResources = $enabled\n  }\n}",
      propertyFullName = "androidResources",
      value = "$enabled",
      assignmentText = "androidResources = $enabled"
    )
    GroovyAndroidGradleParser().parse(testFile) shouldBe AndroidGradleSettings(
      assignments = listOf(
        viewBindingAssignment,
        buildConfigAssignment,
        androidResourcesAssignment
      ),
      androidBlocks = listOf(
        AndroidBlock(
          fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n  }\n  buildFeatures {\n    buildConfig = $enabled\n  }\n}",
          lambdaContent = "buildFeatures {\n    viewBinding = $enabled\n  }\n  buildFeatures {\n    buildConfig = $enabled\n  }\n",
          settings = listOf(
            viewBindingAssignment,
            buildConfigAssignment
          )
        ),
        AndroidBlock(
          fullText = "android {\n  buildFeatures {\n    androidResources = $enabled\n  }\n}",
          lambdaContent = "buildFeatures {\n    androidResources = $enabled\n  }\n",
          settings = listOf(
            androidResourcesAssignment
          )
        )
      ),
      buildFeaturesBlocks = listOf(
        BuildFeaturesBlock(
          fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n  }\n  buildFeatures {\n    buildConfig = $enabled\n  }\n}",
          lambdaContent = "viewBinding = $enabled\n",
          settings = listOf(viewBindingAssignment)
        ),
        BuildFeaturesBlock(
          fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n  }\n  buildFeatures {\n    buildConfig = $enabled\n  }\n}",
          lambdaContent = "buildConfig = $enabled\n",
          settings = listOf(buildConfigAssignment)
        ),
        BuildFeaturesBlock(
          fullText = "android {\n  buildFeatures {\n    androidResources = $enabled\n  }\n}",
          lambdaContent = "androidResources = $enabled\n",
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

    GroovyAndroidGradleParser().parse(testFile) shouldBe AndroidGradleSettings(
      assignments = listOf(
        Assignment(
          fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
          propertyFullName = "viewBinding",
          value = "$enabled",
          assignmentText = "viewBinding = $enabled"
        ),
        Assignment(
          fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
          propertyFullName = "androidResources",
          value = "${!enabled}",
          assignmentText = "androidResources = ${!enabled}"
        )
      ),
      androidBlocks = listOf(
        AndroidBlock(
          fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
          lambdaContent = "buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n",
          settings = listOf(
            Assignment(
              fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
              propertyFullName = "viewBinding",
              value = "$enabled",
              assignmentText = "viewBinding = $enabled"
            ),
            Assignment(
              fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
              propertyFullName = "androidResources",
              value = "${!enabled}",
              assignmentText = "androidResources = ${!enabled}"
            )
          )
        )
      ),
      buildFeaturesBlocks = listOf(
        BuildFeaturesBlock(
          fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
          lambdaContent = "viewBinding = $enabled\n    androidResources = ${!enabled}\n",
          settings = listOf(
            Assignment(
              fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
              propertyFullName = "viewBinding",
              value = "$enabled",
              assignmentText = "viewBinding = $enabled"
            ),
            Assignment(
              fullText = "android {\n  buildFeatures {\n    viewBinding = $enabled\n    androidResources = ${!enabled}\n  }\n}",
              propertyFullName = "androidResources",
              value = "${!enabled}",
              assignmentText = "androidResources = ${!enabled}"
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

    GroovyAndroidGradleParser().parse(testFile) shouldBe AndroidGradleSettings(
      assignments = listOf(
        Assignment(
          fullText = "android.buildFeatures.androidResources = $enabled",
          propertyFullName = "androidResources",
          value = "$enabled",
          assignmentText = "android.buildFeatures.androidResources = $enabled"
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

    GroovyAndroidGradleParser().parse(testFile) shouldBe AndroidGradleSettings(
      assignments = listOf(
        Assignment(
          fullText = "android.buildFeatures {\n  viewBinding = $enabled\n}",
          propertyFullName = "viewBinding",
          value = "$enabled",
          assignmentText = "viewBinding = $enabled"
        )
      ),
      androidBlocks = listOf(),
      buildFeaturesBlocks = listOf(
        BuildFeaturesBlock(
          fullText = "android.buildFeatures {\n  viewBinding = $enabled\n}",
          lambdaContent = "viewBinding = $enabled\n",
          settings = listOf(
            Assignment(
              fullText = "android.buildFeatures {\n  viewBinding = $enabled\n}",
              propertyFullName = "viewBinding",
              value = "$enabled",
              assignmentText = "viewBinding = $enabled"
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

    GroovyAndroidGradleParser().parse(testFile) shouldBe AndroidGradleSettings(
      assignments = listOf(
        Assignment(
          fullText = "android {\n  buildFeatures.viewBinding = $enabled\n}",
          propertyFullName = "viewBinding",
          value = "$enabled",
          assignmentText = "buildFeatures.viewBinding = $enabled"
        )
      ),
      androidBlocks = listOf(
        AndroidBlock(
          fullText = "android {\n  buildFeatures.viewBinding = $enabled\n}",
          lambdaContent = "buildFeatures.viewBinding = $enabled\n",
          settings = listOf(
            Assignment(
              fullText = "android {\n  buildFeatures.viewBinding = $enabled\n}",
              propertyFullName = "viewBinding",
              value = "$enabled",
              assignmentText = "buildFeatures.viewBinding = $enabled"
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
