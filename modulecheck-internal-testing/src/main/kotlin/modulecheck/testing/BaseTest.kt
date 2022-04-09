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
import modulecheck.utils.remove
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.io.File
import kotlin.properties.Delegates

abstract class BaseTest : HermitJUnit5(), FancyShould {

  val testProjectDir: File by resets {
    val className = testInfo.testClass.get()
      // "simpleName" for a nested class is just the nested class name,
      // so use the FqName and trim the package name to get qualified nested names
      .let { it.canonicalName.removePrefix(it.packageName + ".") }
      .split(".")
      .joinToString("/")
      .replace("[^a-zA-Z\\d/]".toRegex(), "_")

    val testName = testInfo.testMethod.get().name
      .replace("[^a-zA-Z0-9]".toRegex(), "_")
      .replace("_{2,}".toRegex(), "_")
      .removeSuffix("_")

    File("build/tests/$className/$testName").absoluteFile
  }

  private var testInfo: TestInfo by Delegates.notNull()

  fun File.relativePath() = path.removePrefix(testProjectDir.path)

  /** Replace CRLF and CR line endings with Unix LF endings.*/
  fun String.normaliseLineSeparators(): String = replace("\r\n|\r".toRegex(), "\n")

  /** Replace Windows file separators with Unix ones, just for string comparison in tests */
  fun String.fixFileSeparators(): String = replace(File.separator, "/")

  fun String.clean(): String {
    return normaliseLineSeparators()
      .fixFileSeparators()
      .useRelativePaths()
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
    return fixFileSeparators()
      .remove(
        // order matters here!!  absolute must go first
        testProjectDir.absolutePath.fixFileSeparators(),
        testProjectDir.path.fixFileSeparators()
      )
  }

  fun test(action: suspend CoroutineScope.() -> Unit) = runBlocking(block = action)

  // This is automatically injected by JUnit5
  @BeforeEach
  internal fun injectTestInfo(testInfo: TestInfo) {
    this.testInfo = testInfo
    testProjectDir.deleteRecursively()
  }

  companion object {
    protected val durationSuffixRegex =
      "(ModuleCheck found \\d+ issues?) in [\\d.]+ seconds\\.[\\s\\S]*".toRegex()
  }
}
