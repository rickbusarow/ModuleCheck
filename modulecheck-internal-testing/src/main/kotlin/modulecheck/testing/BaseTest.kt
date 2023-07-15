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
import modulecheck.utils.mapLines
import modulecheck.utils.noAnsi
import modulecheck.utils.normaliseLineSeparators
import modulecheck.utils.remove
import java.io.File

/** */
@Suppress("UnnecessaryAbstractClass")
abstract class BaseTest<T : TestEnvironment> :
  TrimmedAsserts,
  HasTestEnvironment<T>

/**
 * Replace Windows file separators with Unix ones, just for string comparison in tests
 *
 * @since 0.12.0
 */
fun String.alwaysUnixFileSeparators(): String = replace(File.separator, "/")

/**
 * replace absolute paths with relative ones
 *
 * @since 0.12.0
 */
fun String.useRelativePaths(workingDir: File): String {
  return alwaysUnixFileSeparators()
    .remove(
      // order matters here!!  absolute must go first
      workingDir.absolutePath.alwaysUnixFileSeparators(),
      workingDir.path.alwaysUnixFileSeparators()
    )
}

/**
 * Removes various bits of noise and machine-specific variables from a console or report output.
 * Cleans the provided string by applying various modifications such as normalising line separators,
 * using relative paths, enforcing Unix file separators, and removing specific strings or patterns.
 *
 * @param workingDir The working directory that will be used when making paths relative.
 * @receiver The raw string that needs to be cleaned.
 * @return The cleaned string after all the modifications have been applied.
 */
fun String.clean(workingDir: File): String {
  return normaliseLineSeparators()
    .useRelativePaths(workingDir)
    .alwaysUnixFileSeparators()
    .remove(
      "Type-safe dependency accessors is an incubating feature.",
      "Type-safe project accessors is an incubating feature.",
      "-- ModuleCheck results --",
      "Deprecated Gradle features were used in this build, making it incompatible with Gradle 8.0.",
      "You can use '--warning-mode all' to show the individual deprecation warnings " +
        "and determine if they come from your own scripts or plugins.",
      "To ignore any of these findings, " +
        "annotate the dependency declaration with " +
        "@Suppress(\"<the name of the issue>\") in Kotlin, " +
        "or //noinspection <the name of the issue> in Groovy.",
      "See https://rbusarow.github.io/ModuleCheck/docs/suppressing-findings for more info."
    )
    .remove("in [\\d.]+ seconds\\.".toRegex())
    .noAnsi()
    .mapLines { it.trimEnd() }
    .trimEnd()
    .trimStart('\n')
}
