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

import modulecheck.testing.assert.TrimmedAsserts
import java.io.File

/**
 * Represents a hermetic testing environment with an
 * associated working directory and certain assertions.
 *
 * @param testStackFrame The [StackWalker.StackFrame] from which the test is being run.
 * @param testVariantNames The variant names related to the test.
 */
open class TestEnvironment(
  testStackFrame: StackWalker.StackFrame,
  testVariantNames: List<String>
) : HasWorkingDir(createWorkingDir(testStackFrame, testVariantNames)), TrimmedAsserts {

  constructor(params: TestEnvironmentParams) : this(
    testStackFrame = params.testStackFrame,
    testVariantNames = params.testVariantNames
  )

  /**
   * Asserts that the provided [File] has the expected
   * text after reading it and using relative paths.
   *
   * @param expected The expected text that should be present in the file.
   */
  override infix fun File.shouldHaveText(expected: String) {
    asClueCatching {
      readText().useRelativePaths(workingDir) shouldBe expected
    }
  }

  /**
   * replace absolute paths with relative ones
   *
   * @see modulecheck.testing.useRelativePaths
   */
  fun String.useRelativePaths(): String = useRelativePaths(workingDir)

  /**
   * Cleans the provided string in the context of the [TestEnvironment]'s working directory.
   *
   * @receiver The raw string that needs to be cleaned.
   * @return The cleaned string.
   * @see modulecheck.testing.clean
   */
  fun String.clean(): String = clean(workingDir)
}

/**
 * @property testStackFrame The [StackWalker.StackFrame] from which the test is being run.
 * @property testVariantNames The variant names related to the test.
 */
data class DefaultTestEnvironmentParams(
  override val testStackFrame: StackWalker.StackFrame,
  override val testVariantNames: List<String>
) : TestEnvironmentParams

/** */
interface TestEnvironmentParams {
  /**
   * The [StackWalker.StackFrame] from which the test is being
   * run. Defaults to the current stack frame if not provided.
   */
  val testStackFrame: StackWalker.StackFrame

  /**
   * The variant name(s) related to the test. The first name corresponds to
   * a subdirectory under the directory derived from [testStackFrame]. Each
   * additional name corresponds to a subdirectory inside its predecessor.
   */
  val testVariantNames: List<String>
}
