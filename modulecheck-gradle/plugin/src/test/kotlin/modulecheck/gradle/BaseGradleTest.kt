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

import modulecheck.config.CodeGeneratorBinding
import modulecheck.config.internal.defaultCodeGeneratorBindings
import modulecheck.gradle.TestVersions.Companion.DEFAULT_AGP_VERSION
import modulecheck.gradle.TestVersions.Companion.DEFAULT_ANVIL_VERSION
import modulecheck.gradle.TestVersions.Companion.DEFAULT_GRADLE_VERSION
import modulecheck.gradle.TestVersions.Companion.DEFAULT_KOTLIN_VERSION
import modulecheck.gradle.internal.BuildProperties
import modulecheck.project.ProjectCache
import modulecheck.project.test.ProjectCollector
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

@Suppress("PropertyName")
abstract class BaseGradleTest :
  BaseTest(),
  ProjectCollector,
  DynamicTests,
  VersionsMatrixTest {

  override var kotlinVersion = DEFAULT_KOTLIN_VERSION
  override var agpVersion = DEFAULT_AGP_VERSION
  override var gradleVersion = DEFAULT_GRADLE_VERSION
  override var anvilVersion = DEFAULT_ANVIL_VERSION

  override val projectCache: ProjectCache by resets { ProjectCache() }

  override val root: File
    get() = testProjectDir

  @Suppress("VariableNaming")
  val DEFAULT_BUILD_FILE by resets {
    """
    buildscript {
      dependencies {
        classpath("com.android.tools.build:gradle:$agpVersion")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
      }
    }

    plugins {
      id("com.rickbusarow.module-check")
    }
    """.trimIndent()
  }

  val rootBuild by resets {
    root.child("build.gradle.kts")
      .createSafely(DEFAULT_BUILD_FILE)
  }

  @Suppress("VariableNaming")
  val DEFAULT_SETTINGS_FILE by resets {
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
  }

  override val codeGeneratorBindings: List<CodeGeneratorBinding> = defaultCodeGeneratorBindings()

  val rootSettings by resets {
    root.child("settings.gradle.kts")
      .createSafely(
        DEFAULT_SETTINGS_FILE
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
      // .withTestKitDir(testKitDir)
      // .withPluginClasspath()
      .withDebug(true)
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

  fun build(
    vararg tasks: String,
    withPluginClasspath: Boolean,
    stacktrace: Boolean
  ): BuildResult {
    rootProject
    addIncludes()
    return gradleRunner
      .letIf(withPluginClasspath) { it.withPluginClasspath() }
      .withArguments(tasks.toList().letIf(stacktrace) { it + "--stacktrace" })
      .build()
  }

  fun shouldSucceed(
    vararg tasks: String,
    withPluginClasspath: Boolean = false,
    stacktrace: Boolean = true,
    assertions: BuildResult.() -> Unit = {}
  ): BuildResult {
    val result = build(
      *tasks, withPluginClasspath = withPluginClasspath,
      stacktrace = stacktrace
    )

    result.tasks.last().outcome shouldBe TaskOutcome.SUCCESS

    result.assertions()

    return result
  }

  fun shouldFail(vararg tasks: String): BuildResult {
    rootProject
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

  override fun <T> dynamicTest(
    subject: T,
    testName: String,
    setup: (T) -> Unit,
    action: (T) -> Unit
  ): DynamicTest = DynamicTest.dynamicTest(testName) {
    try {

      testDisplayName = buildString {
        append("$testFunctionName${File.separator}")
        append(testName.replace(" ", "_").remove(":"))
      }

      setup(subject)

      resetAll()
      beforeEach()

      // make sure that the root project is initialized
      rootProject

      action(subject)
    } finally {
      resetAll()
    }
  }
}
