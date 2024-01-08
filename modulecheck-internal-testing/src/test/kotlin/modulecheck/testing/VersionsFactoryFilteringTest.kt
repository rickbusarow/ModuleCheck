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

package modulecheck.testing

import com.rickbusarow.kase.TestEnvironmentFactory
import com.rickbusarow.kase.files.TestLocation
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import modulecheck.testing.VersionFactoryTestTestEnvironment.Factory
import org.junit.jupiter.api.Test

internal class VersionsFactoryFilteringTest :
  VersionsFactoryTest<VersionFactoryTestTestEnvironment, Factory> {

  override val kaseMatrix = McVersionMatrix()

  override val exhaustive: Boolean = true
  override val params: List<McTestVersions>
    get() = kaseMatrix.versions(exhaustive)

  override val testEnvironmentFactory: Factory = Factory()

  @Test
  fun `class-level 'exhaustive' value of true makes the function exhaustive by default`() {

    // the class-level variable can't be changed
    exhaustive shouldBe true

    versions().size shouldBeGreaterThan 1
    versions() shouldBe kaseMatrix.versions(exhaustive = true)
  }

  @Test
  fun `calling 'versions' with 'exhaustive = false' overrides the exhaustive property behavior`() {

    // the class-level variable can't be changed
    exhaustive shouldBe true

    kaseMatrix.versions(false) shouldBe listOf(defaultTestVersions())
  }
}

/** unused */
internal data class VersionFactoryTestTestEnvironment(
  override val testVersions: McTestVersions,
  val testVariantNames: List<String>,
  val testLocation: TestLocation
) : TestEnvironment(testVariantNames, testLocation),
  HasTestVersions {
  class Factory : TestEnvironmentFactory<McTestVersions, VersionFactoryTestTestEnvironment> {

    override fun createEnvironment(
      params: McTestVersions,
      names: List<String>,
      location: TestLocation
    ): VersionFactoryTestTestEnvironment = VersionFactoryTestTestEnvironment(
      testVersions = params,
      testVariantNames = names,
      testLocation = location
    )
  }
}
