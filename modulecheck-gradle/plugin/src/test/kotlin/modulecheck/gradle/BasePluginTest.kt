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

package modulecheck.gradle

import modulecheck.project.test.ProjectTest
import modulecheck.specs.DEFAULT_AGP_VERSION
import modulecheck.specs.DEFAULT_GRADLE_VERSION
import modulecheck.specs.DEFAULT_KOTLIN_VERSION
import modulecheck.utils.child
import modulecheck.utils.letIf
import modulecheck.utils.remove
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.File
import kotlin.text.RegexOption.IGNORE_CASE

abstract class BasePluginTest : ProjectTest() {

  val kotlinVersion = DEFAULT_KOTLIN_VERSION
  val agpVersion = DEFAULT_AGP_VERSION
  val gradleVersion = DEFAULT_GRADLE_VERSION

  // Use a single directory as a cache for the `.gradle` directory used in tests.
  // This saves a significant amount of time by not having to download the distribution each time,
  // and it's also faster than copying all of GRADLE_USER_HOME -- since that directory is massive.
  // To do this, before the test, we copy whatever's in this directory into each test's project
  // directory.  After the test, we copy from the test project directory back to the cache.
  val localGradleCache: File by resets {
    File("build")
      .child("test-gradle-cache")
      .also { it.mkdirs() }
      .absoluteFile
  }

  val testProjectGradleDir: File by resets {
    testProjectDir.child(".gradle")
      .also { it.mkdirs() }
  }

  val gradleRunner by resets {
    GradleRunner.create()
      .forwardOutput()
      .withGradleVersion(gradleVersion)
      .withPluginClasspath()
      // .withDebug(true)
      .withProjectDir(testProjectDir)
  }

  @BeforeEach
  fun beforeEach() {
    testProjectDir.deleteRecursively()

    localGradleCache.copyRecursively(
      testProjectGradleDir,
      overwrite = true
    )
  }

  @AfterEach
  fun afterEach() {
    testProjectGradleDir.copyRecursively(
      localGradleCache,
      overwrite = true
    )
  }

  fun build(vararg tasks: String, stacktrace: Boolean): BuildResult {
    return gradleRunner.withArguments(
      tasks.toList().letIf(stacktrace) { it + "--stacktrace" }
    )
      .build()
  }

  fun BuildResult.shouldSucceed() {
    tasks.last().outcome shouldBe TaskOutcome.SUCCESS
  }

  fun shouldSucceed(
    vararg tasks: String,
    stacktrace: Boolean = true,
    assertions: BuildResult.() -> Unit = {}
  ): BuildResult {
    val result = build(*tasks, stacktrace = stacktrace)

    result.tasks.last().outcome shouldBe TaskOutcome.SUCCESS

    result.assertions()

    return result
  }

  fun shouldFail(vararg tasks: String): BuildResult {
    val result = gradleRunner.withArguments(*tasks).buildAndFail()

    result.tasks.last().outcome shouldBe TaskOutcome.FAILED

    return result
  }

  infix fun BuildResult.withTrimmedMessage(message: String) {
    val trimmed = output
      .clean()
      .remove(
        "FAILURE: Build failed with an exception.",
        "* What went wrong:",
        "* Try:",
        "> Run with --stacktrace option to get the stack trace.",
        "> Run with --info or --debug option to get more log output.",
        "> Run with --scan to get full insights.",
        "* Get more help at https://help.gradle.org",
        "Daemon will be stopped at the end of the build after running out of JVM memory"
      )
      // remove standard Gradle output noise
      .remove(
        "Execution failed for task ':moduleCheck(?:Auto|)\\'.".toRegex(IGNORE_CASE),
        "> Task [^\\n]*".toRegex(),
        ".*Run with --.*".toRegex(),
        "See https://docs\\.gradle\\.org/[^/]+/userguide/command_line_interface\\.html#sec:command_line_warnings".toRegex(),
        "BUILD (?:SUCCESSFUL|FAILED) in .*".toRegex(),
        "\\d+ actionable tasks?: \\d+ executed".toRegex(),
        "> ModuleCheck found \\d+ issues? which (?:was|were) not auto-corrected.".toRegex()
      )
      .removeDuration()
      .remove("\u200B")
      .trim()

    trimmed shouldBe message
  }
}
