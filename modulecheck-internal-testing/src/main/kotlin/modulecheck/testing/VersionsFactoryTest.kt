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

import org.junit.jupiter.api.DynamicTest

/**
 * Convenience interface for a test which uses [VersionsFactory] in order to create [dynamicTest]s
 * for a JUnit5 test factory.
 *
 * @since 0.13.0
 */
interface VersionsFactoryTest : VersionsFactory {

  /**
   * @return the latest version of valid dependencies which is not excluded by the current rules
   * @since 0.13.0
   */
  fun defaultTestVersions(): TestVersions {
    return nonExhaustiveDefaults()
  }

  /**
   * @return a list of [DynamicTest] from all valid versions combinations, optionally filtered by
   *   [filter]. [action] is performed against each element.
   * @since 0.13.0
   */
  fun factory(
    exhaustive: Boolean = this.exhaustive,
    filter: (TestVersions) -> Boolean = { true },
    action: TestVersions.() -> Unit
  ): List<DynamicTest> {

    return versions(exhaustive = exhaustive)
      .filter { filter(it) }
      .map { subject ->
        dynamicTest(subject, subject.toString(), action)
      }
  }

  /**
   * hook for performing setup/teardown for each test within a base test class
   *
   * @since 0.13.0
   */
  fun dynamicTest(
    subject: TestVersions,
    testName: String,
    action: (TestVersions) -> Unit
  ): DynamicTest
}
