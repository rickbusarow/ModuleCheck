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
 * Different functions for creating dynamic tests.
 *
 * @since 0.12.0
 */
interface DynamicTests {

  /**
   * Creates a list of [DynamicTest] instances from a collection of test factory methods.
   *
   * @param testName The name of the test suite.
   * @param test The function to run for each test instance.
   * @return List of dynamic tests.
   */
  fun <T : Any> Iterable<() -> T>.dynamic(testName: String, test: (T) -> Unit): List<DynamicTest> {
    return map { factory -> factory.invoke() }
      .map { subject ->

        DynamicTest.dynamicTest("$testName -- ${subject::class.simpleName}") {
          test.invoke(subject)
        }
      }
  }

  /**
   * Creates a list of [DynamicTest] instances from a sequence of test subjects.
   *
   * The name of each test is determined by the provided [testName] function.
   *
   * @param testName A function that produces a name for each test based on the test subject.
   * @param test The function to run for each test instance.
   * @return List of dynamic tests.
   * @since 0.12.0
   */
  fun <T> Sequence<T>.dynamic(
    testName: (T) -> String = { it.toString() },
    test: (T) -> Unit
  ): List<DynamicTest> = toList().dynamic(testName, test)

  /**
   * Creates a list of [DynamicTest] instances from a collection of test subjects.
   *
   * The name of each test is determined by the provided [testName] function.
   *
   * @param testName A function that produces a name for each test based on the test subject.
   * @param test The function to run for each test instance.
   * @return List of dynamic tests.
   * @since 0.12.0
   */
  fun <T> Iterable<T>.dynamic(
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
