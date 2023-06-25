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

import modulecheck.utils.trace.test.runTestTraced

/** Trait interface for objects that have a [TestEnvironment]. */
interface HasTestEnvironment<T : TestEnvironment> {

  /**
   * Runs the provided test [action] in the context of a new [TestEnvironment].
   *
   * @param testStackFrame The [StackWalker.StackFrame] from which the test is being run.
   * @param testVariantNames The variant names related to the test.
   * @param action The test action to run within the [TestEnvironment].
   */
  @SkipInStackTrace
  fun test(
    testStackFrame: StackWalker.StackFrame = HasWorkingDir.testStackFrame(),
    vararg testVariantNames: String,
    action: suspend T.() -> Unit
  ) {
    test(testStackFrame, testVariantNames.toList(), action = action)
  }

  /**
   * Runs the provided test [action] in the context of a new [TestEnvironment].
   *
   * @param testStackFrame The [StackWalker.StackFrame] from which the test is being run.
   * @param testVariantNames The variant names related to the test.
   * @param action The test action to run within the [TestEnvironment].
   */
  @SkipInStackTrace
  fun test(
    testStackFrame: StackWalker.StackFrame = HasWorkingDir.testStackFrame(),
    testVariantNames: List<String>,
    action: suspend T.() -> Unit
  ) {
    test(
      params = DefaultTestEnvironmentParams(
        testStackFrame = testStackFrame,
        testVariantNames = testVariantNames.toList()
      ),
      action = action
    )
  }

  /**
   * Runs the provided test [action] in the context of a new [TestEnvironment].
   *
   * @param params used to create the [TestEnvironment]
   * @param action The test action to run within the [TestEnvironment].
   */
  @SkipInStackTrace
  fun test(params: TestEnvironmentParams, action: suspend T.() -> Unit) {

    val testEnvironment = newTestEnvironment(params)

    runTestTraced {
      testEnvironment.asClueCatching {
        testEnvironment.action()
        println(testEnvironment)
      }
    }
  }

  /**
   * Creates a new [TestEnvironment].
   *
   * @return A new [TestEnvironment] of type [T].
   */
  fun newTestEnvironment(params: TestEnvironmentParams): T {
    @Suppress("UNCHECKED_CAST")
    return TestEnvironment(params.testStackFrame, params.testVariantNames) as T
  }
}
