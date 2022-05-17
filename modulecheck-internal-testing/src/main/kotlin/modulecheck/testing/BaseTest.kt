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

import hermit.test.junit.HermitJUnit5
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import modulecheck.utils.child
import modulecheck.utils.remove
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.io.File
import kotlin.properties.Delegates

abstract class BaseTest : HermitJUnit5(), FancyShould {

  /**
   * The unique directory for an individual test, recreated each time.
   *
   * For a standard `@Test`-annotated test, this directory will be:
   * `$projectDir/build/tests/$className/$functionName`
   *
   * For a `TestFactory` test, this will be:
   * `$projectDir/build/tests/$className/$functionName/$displayName`
   *
   * This directory is deleted at the **start** of test execution, so it's always fresh, but the
   * source is still there after the test completes.
   */
  val testProjectDir: File by resets {
    File("build")
      .child("tests", testClassName, testDisplayName)
      .absoluteFile
  }

  /** Test class name */
  protected var testClassName: String by Delegates.notNull()

  /** Test function name */
  protected var testFunctionName: String by Delegates.notNull()

  /**
   * This is typically the same as the function name, but for dynamic tests, the name for each
   * permutation is appended.
   */
  protected var testDisplayName: String by Delegates.notNull()

  fun File.relativePath() = path.removePrefix(testProjectDir.path)

  /** Replace CRLF and CR line endings with Unix LF endings. */
  fun String.normaliseLineSeparators(): String = replace("\r\n|\r".toRegex(), "\n")

  /** Replace Windows file separators with Unix ones, just for string comparison in tests */
  fun String.alwaysUnixFileSeparators(): String = replace(File.separator, "/")

  fun String.clean(): String {
    return normaliseLineSeparators()
      .useRelativePaths()
      .alwaysUnixFileSeparators()
      .remove(
        "Type-safe dependency accessors is an incubating feature.",
        "Type-safe project accessors is an incubating feature.",
        "-- ModuleCheck results --",
        "Deprecated Gradle features were used in this build, making it incompatible with Gradle 8.0.",
        "You can use '--warning-mode all' to show the individual deprecation warnings and determine " +
          "if they come from your own scripts or plugins.",
        "To ignore any of these findings, " +
          "annotate the dependency declaration with " +
          "@Suppress(\"<the name of the issue>\") in Kotlin, " +
          "or //noinspection <the name of the issue> in Groovy.",
        "See https://rbusarow.github.io/ModuleCheck/docs/suppressing-findings for more info."
      )
      .remove("in [\\d.]+ seconds\\.".toRegex())
      .trimEnd()
      .trimStart('\n')
  }

  /** replace `ModuleCheck found 2 issues in 1.866 seconds.` with `ModuleCheck found 2 issues` */
  fun String.removeDuration(): String {
    return replace(durationSuffixRegex) { it.destructured.component1() }
  }

  /** replace absolute paths with relative ones */
  fun String.useRelativePaths(): String {
    return alwaysUnixFileSeparators()
      .remove(
        // order matters here!!  absolute must go first
        testProjectDir.absolutePath.alwaysUnixFileSeparators(),
        testProjectDir.path.alwaysUnixFileSeparators()
      )
  }

  fun test(action: suspend CoroutineScope.() -> Unit) = runBlocking(block = action)

  // This is automatically injected by JUnit5
  @BeforeEach
  internal fun injectTestInfo(testInfo: TestInfo) {

    testClassName = testInfo.testClass.get()
      // "simpleName" for a nested class is just the nested class name,
      // so use the FqName and trim the package name to get qualified nested names
      .let { it.canonicalName.removePrefix(it.packageName + ".") }
      .split(".")
      .joinToString(File.separator)
      .replace("[^a-zA-Z\\d/]".toRegex(), "_")

    testFunctionName = testInfo.testMethod.get().name
      .replace("[^a-zA-Z\\d]".toRegex(), "_")
      .replace("_{2,}".toRegex(), "_")
      .removeSuffix("_")

    testDisplayName = testFunctionName

    testProjectDir.deleteRecursively()
  }

  companion object {
    protected val durationSuffixRegex =
      "(ModuleCheck found \\d+ issues?) in [\\d.]+ seconds\\.[\\s\\S]*".toRegex()
  }
}
