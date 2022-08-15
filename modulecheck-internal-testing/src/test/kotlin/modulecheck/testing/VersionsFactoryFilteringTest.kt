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

package modulecheck.testing

import io.kotest.assertions.asClue
import io.kotest.data.row
import io.kotest.matchers.ints.shouldBeGreaterThan
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test

internal class VersionsFactoryFilteringTest : BaseTest(), VersionsFactoryTest {

  override val exhaustive: Boolean
    get() = true

  @Test
  fun `all valid version combinations in the matrix`() {
    versions(true)
      .joinToString(
        separator = ",\n",
        prefix = "########## copy/paste these rows in order to update the test\n",
        postfix = "\n##########"
      ) { (g, ag, av, k) ->
        """row( "$g", "$ag", "$av", "$k")"""
      }
      .asClue {

        versions(true).joinToString("\n") shouldBe listOf(
          row("7.2", "7.0.1", "2.4.1-1-6", "1.6.10"),
          row("7.2", "7.0.1", "2.4.1-1-6", "1.6.21"),
          row("7.2", "7.1.3", "2.4.1", "1.7.0"),
          row("7.2", "7.1.3", "2.4.1", "1.7.10"),
          row("7.4.2", "7.0.1", "2.4.1-1-6", "1.6.10"),
          row("7.4.2", "7.0.1", "2.4.1-1-6", "1.6.21"),
          row("7.5.1", "7.1.3", "2.4.1", "1.7.0"),
          row("7.5.1", "7.1.3", "2.4.1", "1.7.10"),
          row("7.5.1", "7.2.2", "2.4.1", "1.7.0"),
          row("7.5.1", "7.2.2", "2.4.1", "1.7.10")
        )
          .map { (gradle, agp, anvil, kotlin) ->
            TestVersions(
              gradle = gradle,
              agp = agp,
              anvil = anvil,
              kotlin = kotlin
            )
          }
          .joinToString("\n")
      }
  }

  @Test
  fun `class-level 'exhaustive' value of true makes the function exhaustive by default`() {

    // the class-level variable can't be changed
    exhaustive shouldBe true

    versions().size shouldBeGreaterThan 1
    versions() shouldBe versions(exhaustive = true)
  }

  @Test
  fun `calling 'versions' with 'exhaustive = false' overrides the exhaustive property behavior`() {

    // the class-level variable can't be changed
    exhaustive shouldBe true

    versions(false) shouldBe listOf(defaultTestVersions())
  }

  override fun dynamicTest(
    subject: TestVersions,
    testName: String,
    action: (TestVersions) -> Unit
  ): DynamicTest {
    throw NotImplementedError("forced override")
  }
}
