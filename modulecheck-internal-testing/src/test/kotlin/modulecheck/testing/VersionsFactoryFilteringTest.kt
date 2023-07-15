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

package modulecheck.testing

import io.kotest.matchers.ints.shouldBeGreaterThan
import org.junit.jupiter.api.Test
import java.lang.StackWalker.StackFrame

internal class VersionsFactoryFilteringTest :
  BaseTest<VersionFactoryTestTestEnvironment>(),
  VersionsFactoryTest<VersionFactoryTestTestEnvironment> {

  override val exhaustive: Boolean
    get() = true

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

  override fun TestVersions.newParams(stackFrame: StackFrame): TestEnvironmentParams =
    throw NotImplementedError("forced override")
}

/** unused */
internal data class VersionFactoryTestTestEnvironment(
  override val testVersions: TestVersions,
  val testStackFrame: StackWalker.StackFrame,
  val testVariantNames: List<String>
) : TestEnvironment(testStackFrame, testVariantNames),
  HasTestVersions
