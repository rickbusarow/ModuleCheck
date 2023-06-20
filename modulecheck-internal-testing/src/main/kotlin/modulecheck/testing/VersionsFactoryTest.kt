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
import modulecheck.utils.requireNotNull
import org.junit.jupiter.api.DynamicTest
import java.lang.StackWalker.StackFrame

/**
 * Convenience interface for a test which uses [VersionsFactory]
 * in order to create [dynamicTest]s for a JUnit5 test factory.
 */
interface VersionsFactoryTest<T> :
  VersionsFactory,
  HasTestEnvironment<T>
  where T : TestEnvironment,
        T : HasTestVersions {

  /** @return the latest version of valid dependencies which is not excluded by the current rules */
  fun defaultTestVersions(): TestVersions {
    return nonExhaustiveDefaults()
  }

  /**
   * @return a list of [DynamicTest] from all valid versions combinations,
   *   optionally filtered by [filter]. [action] is performed against each element.
   */
  @SkipInStackTrace
  fun factory(
    exhaustive: Boolean = this.exhaustive,
    filter: ((TestVersions) -> Boolean)? = null,
    action: suspend T.() -> Unit
  ): List<DynamicTest> {

    val stackFrame = HasWorkingDir.testStackFrame()

    return versions(exhaustive = exhaustive)
      .letIf(filter != null) { versions ->

        val (included, excluded) = allVersions
          .partition(filter.requireNotNull())

        "The filter excludes all possible versions".asClue {
          included.shouldNotBeEmpty()
        }

        "The filter does not exclude any versions".asClue {
          excluded.shouldNotBeEmpty()
        }

        versions.filter(filter.requireNotNull())
      }
      .map { subject ->
        dynamicTest(subject, stackFrame, subject.toString(), action)
      }
  }

  /** hook for performing setup/teardown for each test within a base test class */
  fun dynamicTest(
    subject: TestVersions,
    stackFrame: StackFrame,
    testName: String,
    action: suspend T.() -> Unit
  ): DynamicTest
}
