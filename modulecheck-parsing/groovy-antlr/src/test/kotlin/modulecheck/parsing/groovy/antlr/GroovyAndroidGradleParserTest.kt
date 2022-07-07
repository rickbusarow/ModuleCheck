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

package modulecheck.parsing.groovy.antlr

import kotlinx.coroutines.runBlocking
import modulecheck.parsing.gradle.dsl.AndroidGradleSettings
import modulecheck.parsing.gradle.dsl.AndroidGradleSettings.AgpBlock.AndroidBlock
import modulecheck.parsing.gradle.dsl.AndroidGradleSettings.AgpBlock.BuildFeaturesBlock
import modulecheck.parsing.gradle.dsl.Assignment
import modulecheck.testing.BaseTest
import modulecheck.utils.child
import modulecheck.utils.createSafely
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

internal class GroovyAndroidGradleParserTest : BaseTest() {

  val testFile by resets {
    testProjectDir.child("build.gradle").createSafely()
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
      fullText = "android {\n" +
        "  buildFeatures {\n" +
        "    viewBinding = $enabled\n" +
        "  }\n" +
        "  buildFeatures {\n" +
        "    buildConfig = $enabled\n" +
        "  }\n" +
        "}",
      propertyFullName = "viewBinding",
      value = "$enabled",
      declarationText = "viewBinding = $enabled",
      suppressed = listOf()
    )
    val buildConfigAssignment = Assignment(
      fullText = "android {\n" +
        "  buildFeatures {\n" +
        "    viewBinding = $enabled\n" +
        "  }\n" +
        "  buildFeatures {\n" +
        "    buildConfig = $enabled\n" +
        "  }\n" +
        "}",
      propertyFullName = "buildConfig",
      value = "$enabled",
      declarationText = "buildConfig = $enabled",
      suppressed = listOf()
    )
    val androidResourcesAssignment = Assignment(
      fullText = "android {\n" +
        "  buildFeatures {\n" +
        "    androidResources = $enabled\n" +
        "  }\n" +
        "}",
      propertyFullName = "androidResources",
      value = "$enabled",
      declarationText = "androidResources = $enabled",
      suppressed = listOf()
    )

    GroovyAndroidGradleParser().parse(testFile) shouldBe AndroidGradleSettings(
      assignments = listOf(
        viewBindingAssignment,
        buildConfigAssignment,
        androidResourcesAssignment
      ),
      androidBlocks = listOf(
        AndroidBlock(
          fullText = "android {\n" +
            "  buildFeatures {\n" +
            "    viewBinding = $enabled\n" +
            "  }\n" +
            "  buildFeatures {\n" +
            "    buildConfig = $enabled\n" +
            "  }\n" +
            "}",
          lambdaContent = "buildFeatures {\n" +
            "    viewBinding = $enabled\n" +
            "  }\n" +
            "  buildFeatures {\n" +
            "    buildConfig = $enabled\n" +
            "  }\n",
          settings = listOf(
            viewBindingAssignment,
            buildConfigAssignment
          ),
          blockSuppressed = listOf()
        ),
        AndroidBlock(
          fullText = "android {\n" +
            "  buildFeatures {\n" +
            "    androidResources = $enabled\n" +
            "  }\n" +
            "}",
          lambdaContent = "buildFeatures {\n" +
            "    androidResources = $enabled\n" +
            "  }\n",
          settings = listOf(
            androidResourcesAssignment
          ),
          blockSuppressed = listOf()
        )
      ),
      buildFeaturesBlocks = listOf(
        BuildFeaturesBlock(
          fullText = "android {\n" +
            "  buildFeatures {\n" +
            "    viewBinding = $enabled\n" +
            "  }\n" +
            "  buildFeatures {\n" +
            "    buildConfig = $enabled\n" +
            "  }\n" +
            "}",
          lambdaContent = "viewBinding = $enabled\n",
          settings = listOf(viewBindingAssignment),
          blockSuppressed = listOf()
        ),
        BuildFeaturesBlock(
          fullText = "android {\n" +
            "  buildFeatures {\n" +
            "    viewBinding = $enabled\n" +
            "  }\n" +
            "  buildFeatures {\n" +
            "    buildConfig = $enabled\n" +
            "  }\n" +
            "}",
          lambdaContent = "buildConfig = $enabled\n",
          settings = listOf(buildConfigAssignment),
          blockSuppressed = listOf()
        ),
        BuildFeaturesBlock(
          fullText = "android {\n" +
            "  buildFeatures {\n" +
            "    androidResources = $enabled\n" +
            "  }\n" +
            "}",
          lambdaContent = "androidResources = $enabled\n",
          settings = listOf(androidResourcesAssignment),
          blockSuppressed = listOf()
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
          fullText = "android {\n" +
            "  buildFeatures {\n" +
            "    viewBinding = $enabled\n" +
            "    androidResources = ${!enabled}\n" +
            "  }\n" +
            "}",
          propertyFullName = "viewBinding",
          value = "$enabled",
          declarationText = "viewBinding = $enabled",
          suppressed = listOf()
        ),
        Assignment(
          fullText = "android {\n" +
            "  buildFeatures {\n" +
            "    viewBinding = $enabled\n" +
            "    androidResources = ${!enabled}\n" +
            "  }\n" +
            "}",
          propertyFullName = "androidResources",
          value = "${!enabled}",
          declarationText = "androidResources = ${!enabled}",
          suppressed = listOf()
        )
      ),
      androidBlocks = listOf(
        AndroidBlock(
          fullText = "android {\n" +
            "  buildFeatures {\n" +
            "    viewBinding = $enabled\n" +
            "    androidResources = ${!enabled}\n" +
            "  }\n" +
            "}",
          lambdaContent = "buildFeatures {\n" +
            "    viewBinding = $enabled\n" +
            "    androidResources = ${!enabled}\n" +
            "  }\n",
          settings = listOf(
            Assignment(
              fullText = "android {\n" +
                "  buildFeatures {\n" +
                "    viewBinding = $enabled\n" +
                "    androidResources = ${!enabled}\n" +
                "  }\n" +
                "}",
              propertyFullName = "viewBinding",
              value = "$enabled",
              declarationText = "viewBinding = $enabled",
              suppressed = listOf()
            ),
            Assignment(
              fullText = "android {\n" +
                "  buildFeatures {\n" +
                "    viewBinding = $enabled\n" +
                "    androidResources = ${!enabled}\n" +
                "  }\n" +
                "}",
              propertyFullName = "androidResources",
              value = "${!enabled}",
              declarationText = "androidResources = ${!enabled}",
              suppressed = listOf()
            )
          ),
          blockSuppressed = listOf()
        )
      ),
      buildFeaturesBlocks = listOf(
        BuildFeaturesBlock(
          fullText = "android {\n" +
            "  buildFeatures {\n" +
            "    viewBinding = $enabled\n" +
            "    androidResources = ${!enabled}\n" +
            "  }\n" +
            "}",
          lambdaContent = "viewBinding = $enabled\n" +
            "    androidResources = ${!enabled}\n",
          settings = listOf(
            Assignment(
              fullText = "android {\n" +
                "  buildFeatures {\n" +
                "    viewBinding = $enabled\n" +
                "    androidResources = ${!enabled}\n" +
                "  }\n" +
                "}",
              propertyFullName = "viewBinding",
              value = "$enabled",
              declarationText = "viewBinding = $enabled",
              suppressed = listOf()
            ),
            Assignment(
              fullText = "android {\n" +
                "  buildFeatures {\n" +
                "    viewBinding = $enabled\n" +
                "    androidResources = ${!enabled}\n" +
                "  }\n" +
                "}",
              propertyFullName = "androidResources",
              value = "${!enabled}",
              declarationText = "androidResources = ${!enabled}",
              suppressed = listOf()
            )
          ),
          blockSuppressed = listOf()
        )
      )
    )
  }

  @TestFactory
  fun `fully dot qualified boolean property`() = runTest { enabled ->

    val block = """
      plugins {
        id 'my-plugin'
      }
      //noinspection android-level
      android.buildFeatures.androidResources = $enabled
    """.trimIndent()

    testFile.writeText(block)

    GroovyAndroidGradleParser().parse(testFile) shouldBe AndroidGradleSettings(
      assignments = listOf(
        Assignment(
          fullText = "//noinspection android-level\nandroid.buildFeatures.androidResources = $enabled",
          propertyFullName = "androidResources",
          value = "$enabled",
          declarationText = "android.buildFeatures.androidResources = $enabled",
          suppressed = listOf("android-level")
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
        //noinspection assignment-level
        viewBinding = $enabled
      }
    """.trimIndent()

    testFile.writeText(block)

    GroovyAndroidGradleParser().parse(testFile) shouldBe AndroidGradleSettings(
      assignments = listOf(
        Assignment(
          fullText = "android.buildFeatures {\n" +
            "  //noinspection assignment-level\n" +
            "  viewBinding = $enabled\n" +
            "}",
          propertyFullName = "viewBinding",
          value = "$enabled",
          declarationText = "viewBinding = $enabled",
          suppressed = listOf("assignment-level")
        )
      ),
      androidBlocks = listOf(),
      buildFeaturesBlocks = listOf(
        BuildFeaturesBlock(
          fullText = "android.buildFeatures {\n" +
            "  //noinspection assignment-level\n" +
            "  viewBinding = $enabled\n" +
            "}",
          lambdaContent = "viewBinding = $enabled\n",
          settings = listOf(
            Assignment(
              fullText = "android.buildFeatures {\n" +
                "  //noinspection assignment-level\n" +
                "  viewBinding = $enabled\n" +
                "}",
              propertyFullName = "viewBinding",
              value = "$enabled",
              declarationText = "viewBinding = $enabled",
              suppressed = listOf("assignment-level")
            )
          ),
          blockSuppressed = listOf()
        )
      )
    )
  }

  @TestFactory
  fun `scoped and then dot qualified boolean property`() = runTest { enabled ->

    val block = """
      android {
        //noinspection buildFeatures-level
        buildFeatures.viewBinding = $enabled
      }
    """.trimIndent()

    testFile.writeText(block)

    GroovyAndroidGradleParser().parse(testFile) shouldBe AndroidGradleSettings(
      assignments = listOf(
        Assignment(
          fullText = "android {\n" +
            "  //noinspection buildFeatures-level\n" +
            "  buildFeatures.viewBinding = $enabled\n" +
            "}",
          propertyFullName = "viewBinding",
          value = "$enabled",
          declarationText = "buildFeatures.viewBinding = $enabled",
          suppressed = listOf("buildFeatures-level")
        )
      ),
      androidBlocks = listOf(
        AndroidBlock(
          fullText = "android {\n" +
            "  //noinspection buildFeatures-level\n" +
            "  buildFeatures.viewBinding = $enabled\n" +
            "}",
          lambdaContent = "buildFeatures.viewBinding = $enabled\n",
          settings = listOf(
            Assignment(
              fullText = "android {\n" +
                "  //noinspection buildFeatures-level\n" +
                "  buildFeatures.viewBinding = $enabled\n" +
                "}",
              propertyFullName = "viewBinding",
              value = "$enabled",
              declarationText = "buildFeatures.viewBinding = $enabled",
              suppressed = listOf("buildFeatures-level")
            )
          ),
          blockSuppressed = listOf()
        )
      ),
      buildFeaturesBlocks = listOf()
    )
  }

  fun runTest(block: suspend (enabled: Boolean) -> Unit): List<DynamicTest> {
    return listOf(true, false).map { enabled ->

      val paramsString = " -- enabled: $enabled"

      val name = "${testDisplayName.replace("()", "")}$paramsString"

      DynamicTest.dynamicTest(name) {
        runBlocking { block(enabled) }
        resetAll()

        System.gc()
      }
    }
  }
}
