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

import com.rickbusarow.kase.DefaultTestEnvironment
import com.rickbusarow.kase.TestEnvironment
import com.rickbusarow.kase.files.TestLocation
import modulecheck.testing.assert.TrimmedAsserts
import java.io.File

/** */
open class TestEnvironment(
  names: List<String>,
  testLocation: TestLocation = TestLocation.get()
) : TestEnvironment by DefaultTestEnvironment(names = names, testLocation = testLocation),
  TrimmedAsserts {

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

/** */
interface TestEnvironmentParams
