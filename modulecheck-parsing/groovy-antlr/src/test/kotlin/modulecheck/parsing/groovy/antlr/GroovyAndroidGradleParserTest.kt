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

package modulecheck.parsing.groovy.antlr

import modulecheck.parsing.gradle.dsl.AndroidGradleSettings
import modulecheck.parsing.gradle.dsl.AndroidGradleSettings.AgpBlock.AndroidBlock
import modulecheck.parsing.gradle.dsl.AndroidGradleSettings.AgpBlock.BuildFeaturesBlock
import modulecheck.parsing.gradle.dsl.Assignment
import modulecheck.testing.BaseTest
import modulecheck.testing.DynamicTests
import modulecheck.testing.HasWorkingDir
import modulecheck.testing.SkipInStackTrace
import modulecheck.testing.TestEnvironment
import modulecheck.utils.createSafely
import modulecheck.utils.resolve
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File

internal class GroovyAndroidGradleParserTest : BaseTest<TestEnvironment>(), DynamicTests {

  val TestEnvironment.testFile: File
    get() = workingDir.resolve("build.gradle").createSafely()

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
      suppressed = emptyList()
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
      suppressed = emptyList()
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
      suppressed = emptyList()
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
          blockSuppressed = emptyList()
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
          blockSuppressed = emptyList()
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
          blockSuppressed = emptyList()
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
          blockSuppressed = emptyList()
        ),
        BuildFeaturesBlock(
          fullText = "android {\n" +
            "  buildFeatures {\n" +
            "    androidResources = $enabled\n" +
            "  }\n" +
            "}",
          lambdaContent = "androidResources = $enabled\n",
          settings = listOf(androidResourcesAssignment),
          blockSuppressed = emptyList()
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
          suppressed = emptyList()
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
          suppressed = emptyList()
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
              suppressed = emptyList()
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
              suppressed = emptyList()
            )
          ),
          blockSuppressed = emptyList()
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
              suppressed = emptyList()
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
              suppressed = emptyList()
            )
          ),
          blockSuppressed = emptyList()
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
      androidBlocks = emptyList(),
      buildFeaturesBlocks = emptyList()
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
      androidBlocks = emptyList(),
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
          blockSuppressed = emptyList()
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
          blockSuppressed = emptyList()
        )
      ),
      buildFeaturesBlocks = emptyList()
    )
  }

  @SkipInStackTrace
  fun runTest(block: suspend TestEnvironment.(enabled: Boolean) -> Unit): List<DynamicTest> {

    val frame = HasWorkingDir.testStackFrame()

    return listOf(true, false)
      .dynamic({ "enabled: $it" }) { enabled ->

        test(
          testStackFrame = frame,
          "enabled_$enabled"
        ) {
          block(enabled)
        }
      }
  }
}
