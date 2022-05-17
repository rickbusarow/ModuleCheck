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

import modulecheck.gradle.internal.BuildProperties
import modulecheck.project.ProjectCache
import modulecheck.project.test.ProjectCollector
import modulecheck.specs.DEFAULT_AGP_VERSION
import modulecheck.specs.DEFAULT_ANVIL_VERSION
import modulecheck.specs.DEFAULT_GRADLE_VERSION
import modulecheck.specs.DEFAULT_KOTLIN_VERSION
import modulecheck.testing.BaseTest
import modulecheck.testing.DynamicTests
import modulecheck.utils.child
import modulecheck.utils.createSafely
import modulecheck.utils.letIf
import modulecheck.utils.remove
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import java.io.File
import kotlin.text.RegexOption.IGNORE_CASE

abstract class BaseGradleTest : BaseTest(), ProjectCollector, DynamicTests {

  var kotlinVersion = DEFAULT_KOTLIN_VERSION
  var agpVersion = DEFAULT_AGP_VERSION
  var gradleVersion = DEFAULT_GRADLE_VERSION
  var anvilVersion = DEFAULT_ANVIL_VERSION

  val kotlinVersions = sequenceOf("1.5.32", "1.6.21", "1.7.0-Beta")
  val gradleVersions = listOf("7.3.3", "7.4.2", "7.5-rc-1")
  val agpVersions = listOf("7.0.3", "7.1.3", "7.2.0")
  val anvilVersions = listOf("2.3.11", "2.4.0")

  override val projectCache: ProjectCache by resets { ProjectCache() }

  override val root: File
    get() = testProjectDir

  val rootBuild by resets {
    root.child("build.gradle.kts")
      .createSafely(
        """
        plugins {
          id("com.rickbusarow.module-check")
        }
        """.trimIndent()
      )
  }
  val rootSettings by resets {
    root.child("settings.gradle.kts")
      .createSafely(
        """
        rootProject.name = "root"

        pluginManagement {
          repositories {
            gradlePluginPortal()
            mavenCentral()
            mavenLocal()
            google()
          }
          resolutionStrategy {
            eachPlugin {
              if (requested.id.id.startsWith("com.android")) {
                useVersion("$agpVersion")
              }
              if (requested.id.id == "com.rickbusarow.module-check") {
                useVersion("${BuildProperties.VERSION}")
              }
              if (requested.id.id.startsWith("org.jetbrains.kotlin")) {
                useVersion("$kotlinVersion")
              }
              if (requested.id.id == "com.squareup.anvil") {
                useVersion("$anvilVersion")
              }
            }
          }
        }
        dependencyResolutionManagement {
          @Suppress("UnstableApiUsage")
          repositories {
            mavenCentral()
            mavenLocal()
            google()
          }
        }
        """.trimIndent()
      )
  }

  val rootProject by resets {
    rootBuild
    rootSettings
    root
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
  }

  // Make sure that every project in the cache is also added to the root project's settings file
  private fun addIncludes() {
    val includes = projectCache.values.map { it.path.value }
      .joinToString(separator = "\n", prefix = "\n", postfix = "\n") { "include(\"$it\")" }
    rootSettings.appendText(includes)
  }

  fun build(vararg tasks: String, stacktrace: Boolean): BuildResult {
    addIncludes()
    return gradleRunner
      .withArguments(tasks.toList().letIf(stacktrace) { it + "--stacktrace" })
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
    addIncludes()
    val result = gradleRunner.withArguments(*tasks)
      .buildAndFail()

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

  fun gradle(action: () -> Unit): List<DynamicTest> {

    return gradleVersions.dynamic({ "gradle $it" }, { gradleVersion = it }, action)
  }

  fun dynamic(action: () -> Unit): List<DynamicTest> {

    val combinations =
      gradleVersions.flatMap { gradleVersion ->
        agpVersions.flatMap { agpVersion ->
          kotlinVersions.map { kotlinVersion ->
            TestVersions(gradleVersion, agpVersion, kotlinVersion)
          }
        }
      }

    return combinations.toList().dynamic(
      testName = { it.toString() },
      setup = { subject ->
        agpVersion = subject.agpVersion
        gradleVersion = subject.gradleVersion
        kotlinVersion = subject.kotlinVersion
      },
      action = action
    )
  }

  private fun <T> List<T>.dynamic(
    testName: (T) -> String,
    setup: (T) -> Unit,
    action: () -> Unit
  ): List<DynamicTest> {

    val baseName = testFunctionName

    return map { subject ->

      DynamicTest.dynamicTest(testName(subject)) {
        try {

          testDisplayName = buildString {
            append("$baseName${File.separator}")
            append(testName(subject).replace(" ", "_").remove(":"))
          }

          setup(subject)

          beforeEach()

          // make sure that the root project is initialized
          rootProject

          action()
        } finally {
          resetAll()
        }
      }
    }
  }

  data class TestVersions(
    val gradleVersion: String,
    val agpVersion: String,
    val kotlinVersion: String
  ) {
    override fun toString(): String {
      return "[gradle $gradleVersion, agp $agpVersion, kotlin $kotlinVersion]"
    }
  }
}
