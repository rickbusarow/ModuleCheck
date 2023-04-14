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

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldNotBeEmpty
import modulecheck.utils.letIf
import org.junit.jupiter.api.DynamicTest

/**
 * Convenience interface for a test which uses [VersionsFactory]
 * in order to create [dynamicTest]s for a JUnit5 test factory.
 */
interface VersionsFactoryTest : VersionsFactory {

  /**
   * @return the latest version of valid dependencies which is not excluded by the current rules
   */
  fun defaultTestVersions(): TestVersions {
    return nonExhaustiveDefaults()
  }

  /**
   * @return a list of [DynamicTest] from all valid versions combinations,
   *   optionally filtered by [filter]. [action] is performed against each element.
   */
  fun factory(
    exhaustive: Boolean = this.exhaustive,
    filter: ((TestVersions) -> Boolean)? = null,
    action: TestVersions.() -> Unit
  ): List<DynamicTest> {

    if (filter != null) {

      val (included, excluded) = versions(exhaustive = true)
        .partition(filter)

      "The filter excludes all possible versions".asClue {
        included.shouldNotBeEmpty()
      }

      "The filter does not exclude any versions".asClue {
        excluded.shouldNotBeEmpty()
      }
    }

    return versions(exhaustive = exhaustive)
      .letIf(filter != null) {
        it.filter(filter!!)
      }
      .map { subject ->
        dynamicTest(subject, subject.toString(), action)
      }
  }

  /**
   * hook for performing setup/teardown for each test within a base test class
   *
   */
  fun dynamicTest(
    subject: TestVersions,
    testName: String,
    action: (TestVersions) -> Unit
  ): DynamicTest
}
