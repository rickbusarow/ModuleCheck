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

package modulecheck.core

import hermit.test.junit.HermitJUnit5
import hermit.test.resets
import io.kotest.matchers.shouldBe
import modulecheck.psi.AndroidBuildFeaturesVisitor
import modulecheck.psi.internal.asKtFile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInfo

internal class AndroidBuildFeaturesVisitorTest : HermitJUnit5() {

  val testFile by tempFile()
  val testFileKt by resets { testFile.asKtFile() }

  val visitor by resets { AndroidBuildFeaturesVisitor() }

  private var testInfo: TestInfo? = null

  @BeforeEach
  fun beforeEach(testInfo: TestInfo) {
    this.testInfo = testInfo
  }

  @TestFactory
  fun `should find when scoped twice`() = runTest { enabled ->

    val block = """
      android {
        buildFeatures {
          viewBinding = $enabled
        }
      }
    """.trimIndent()

    testFile.writeText(block)

    visitor.find(testFileKt, "viewBinding").toString() shouldBe block
  }

  @TestFactory
  fun `should find when scoped then dot qualified`() = runTest { enabled ->

    val block = """
      android {
        buildFeatures.viewBinding = $enabled
      }
    """.trimIndent()

    testFile.writeText(block)

    visitor.find(testFileKt, "viewBinding").toString() shouldBe block
  }

  @TestFactory
  fun `should find when dot qualified then scoped`() = runTest { enabled ->

    val block = """
      android.buildFeatures {
        viewBinding = $enabled
        androidResources = $enabled
      }
    """.trimIndent()

    testFile.writeText(block)

    visitor.find(testFileKt, "viewBinding").toString() shouldBe block
  }

  @TestFactory
  fun `should find when dot qualified then scoped without line breaks`() = runTest { enabled ->

    val block =
      """
      android.buildFeatures { viewBinding = $enabled }
      """.trimIndent()

    testFile.writeText(block)

    visitor.find(testFileKt, "viewBinding").toString() shouldBe block
  }

  @TestFactory
  fun `should find when fully dot qualified`() = runTest { enabled ->

    testFile.writeText(
      """
      android.buildFeatures.viewBinding = $enabled
      """.trimIndent()
    )

    visitor.find(testFileKt, "viewBinding").toString() shouldBe "android.buildFeatures.viewBinding = $enabled"
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
