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

  fun BuildResult.shouldFail(): BuildResult = apply {
    tasks.last().outcome shouldBe TaskOutcome.FAILED
  }

  fun shouldFail(vararg tasks: String): BuildResult {
    val result = gradleRunner.withArguments(*tasks).buildAndFail()

    result.tasks.last().outcome shouldBe TaskOutcome.FAILED

    return result
  }

  infix fun BuildResult.withTrimmedMessage(message: String) {
    val trimmed = output
      .fixPath() // normalize path separators
      .replace(testProjectDir.absolutePath, "") // replace absolute paths with relative ones
      .let {
        staticPrefixes.fold(it) { acc, prefix ->
          acc.replace(prefix, "")
        }
      }
      .replace(prefixRegex, "") // remove the stuff which'll always be there
      // replace `ModuleCheck found 2 issues in 1.866 seconds.` with `ModuleCheck found 2 issues`
      .replace(suffixRegex) { it.destructured.component1() }
      .trim()

    trimmed shouldBe message
  }

  companion object {

    val staticPrefixes = listOf(
      "Type-safe dependency accessors is an incubating feature.",
      "Type-safe project accessors is an incubating feature.",
      "-- ModuleCheck results --"
    )

    val prefixRegex = "> Task [^\\n]*".toRegex()

    val suffixRegex = "(ModuleCheck found [\\d]+ issues) in [\\d\\.]+ seconds\\.[\\s\\S]*".toRegex()
  }
}
