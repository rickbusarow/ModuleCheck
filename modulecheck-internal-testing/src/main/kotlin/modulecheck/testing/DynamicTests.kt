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

import hermit.test.ResetManager
import org.junit.jupiter.api.DynamicTest

/**
 * Different functions for creating dynamic tests.
 *
 * @since 0.12.0
 */
interface DynamicTests : ResetManager {

  fun <T : Any> Iterable<() -> T>.dynamic(
    testName: String,
    test: (T) -> Unit
  ): List<DynamicTest> {
    return map { factory -> factory.invoke() }
      .map { subject ->

        DynamicTest.dynamicTest("$testName -- ${subject::class.simpleName}") {
          test.invoke(subject)
        }
      }
  }

  /**
   * @return a collection of [DynamicTest], named after [testName] for each test and executing the
   *   logic within [test]
   * @since 0.12.0
   */
  fun <T : Any> Sequence<T>.dynamic(
    testName: (T) -> String = { it.toString() },
    test: (T) -> Unit
  ): List<DynamicTest> = toList().dynamic(testName, test)

  /**
   * @return a collection of [DynamicTest], named after [testName] for each test and executing the
   *   logic within [test]
   * @since 0.12.0
   */
  fun <T : Any> Iterable<T>.dynamic(
    testName: (T) -> String = { it.toString() },
    test: (T) -> Unit
  ): List<DynamicTest> {
    return map { subject ->
      DynamicTest.dynamicTest("${testName(subject)} -- $subject") {
        test.invoke(subject)
      }
    }
  }
}
