/*
 * Copyright (C) 2021 Rick Busarow
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

package modulecheck.gradle

import modulecheck.specs.DEFAULT_AGP_VERSION
import modulecheck.specs.DEFAULT_GRADLE_VERSION
import modulecheck.specs.DEFAULT_KOTLIN_VERSION
import modulecheck.testing.BaseTest
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import kotlin.properties.Delegates

abstract class BasePluginTest : BaseTest() {

  val kotlinVersion = DEFAULT_KOTLIN_VERSION
  val agpVersion = DEFAULT_AGP_VERSION
  val gradleVersion = DEFAULT_GRADLE_VERSION

  val gradleRunner by resets {
    GradleRunner.create()
      .forwardOutput()
      .withGradleVersion(gradleVersion)
      .withPluginClasspath()
      // .withDebug(true)
      .withProjectDir(testProjectDir)
  }

  private var testInfo: TestInfo by Delegates.notNull()

  @BeforeEach
  fun beforeEach() {
    testProjectDir.deleteRecursively()
  }

  fun build(vararg tasks: String): BuildResult {
    return gradleRunner.withArguments(*tasks).build()
  }

  fun BuildResult.shouldSucceed() {
    tasks.last().outcome shouldBe TaskOutcome.SUCCESS
  }

  fun shouldSucceed(vararg tasks: String): BuildResult {
    val result = gradleRunner.withArguments(*tasks).build()

    result.tasks.last().outcome shouldBe TaskOutcome.SUCCESS

    return result
  }

  fun shouldFail(vararg tasks: String): BuildResult {
    val result = gradleRunner.withArguments(*tasks).buildAndFail()

    result.tasks.last().outcome shouldBe TaskOutcome.FAILED

    return result
  }

  infix fun BuildResult.withTrimmedMessage(message: String) {

    println("******************************************\n${testProjectDir.absolutePath}\n\n***************************************************")

    val trimmed = output
      .replace(testProjectDir.absolutePath, "") // replace absolute paths with relative ones
      .fixPath() // normalize path separators
      .let {
        staticPrefixes.fold(it) { acc, prefix ->
          acc.replace(prefix, "")
        }
      }
      .let {
        prefixRegexes.fold(it) { acc, prefix ->
          acc.replace(prefix, "")
        }
      }
      // replace `ModuleCheck found 2 issues in 1.866 seconds.` with `ModuleCheck found 2 issues`
      .replace(suffixRegex) { it.destructured.component1() }
      .trim()

    trimmed shouldBe message
  }

  companion object {

    val staticPrefixes = listOf(
      "Type-safe dependency accessors is an incubating feature.",
      "Type-safe project accessors is an incubating feature.",
      "-- ModuleCheck results --",
      "Deprecated Gradle features were used in this build, making it incompatible with Gradle 8.0.",
      "You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins."
    )

    val prefixRegexes = listOf(
      "> Task [^\\n]*".toRegex(),
      "See https://docs\\.gradle\\.org/[^/]+/userguide/command_line_interface\\.html#sec:command_line_warnings".toRegex(),
      "BUILD SUCCESSFUL in \\d+m?s".toRegex(),
      "\\d+ actionable tasks?: \\d+ executed".toRegex()
    )

    val suffixRegex = "(ModuleCheck found [\\d]+ issues?) in [\\d\\.]+ seconds\\.[\\s\\S]*".toRegex()
  }
}
