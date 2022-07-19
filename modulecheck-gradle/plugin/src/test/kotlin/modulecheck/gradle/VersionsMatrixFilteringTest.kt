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

package modulecheck.gradle

import io.kotest.data.row
import modulecheck.testing.BaseTest
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test

internal class VersionsMatrixFilteringTest : BaseTest(), VersionsMatrixTest {

  override var kotlinVersion = TestVersions.DEFAULT_KOTLIN_VERSION
  override var agpVersion = TestVersions.DEFAULT_AGP_VERSION
  override var gradleVersion = TestVersions.DEFAULT_GRADLE_VERSION
  override var anvilVersion = TestVersions.DEFAULT_ANVIL_VERSION

  @Test
  fun `all valid version combinations in the matrix`() {
    testProjectVersions().forEach { (g, ag, k, av) ->
      println("""row( "$g", "$ag","$k","$av"),""")
    }

    testProjectVersions() shouldBe listOf(
      row("7.4.2", "7.0.1", "1.6.21", "2.4.1-1-6"),
      row("7.4.2", "7.0.1", "1.7.0", "2.4.1"),
      row("7.4.2", "7.0.1", "1.7.10", "2.4.1"),
      row("7.4.2", "7.1.3", "1.6.10", "2.4.1-1-6"),
      row("7.4.2", "7.1.3", "1.6.21", "2.4.1-1-6"),
      row("7.4.2", "7.1.3", "1.7.0", "2.4.1"),
      row("7.4.2", "7.1.3", "1.7.10", "2.4.1"),
      row("7.4.2", "7.2.1", "1.6.10", "2.4.1-1-6"),
      row("7.4.2", "7.2.1", "1.6.21", "2.4.1-1-6"),
      row("7.4.2", "7.2.1", "1.7.0", "2.4.1"),
      row("7.4.2", "7.2.1", "1.7.10", "2.4.1")
    )
      .map { (gradle, agp, kotlin, anvil) ->
        TestVersions(gradle, agp, kotlin, anvil)
      }
  }

  override fun <T> dynamicTest(
    subject: T,
    testName: String,
    setup: (T) -> Unit,
    action: (T) -> Unit
  ): DynamicTest {
    throw NotImplementedError("forced override")
  }
}
