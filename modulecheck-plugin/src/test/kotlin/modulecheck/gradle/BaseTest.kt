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

import hermit.test.junit.HermitJUnit5
import hermit.test.resets
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import modulecheck.specs.ProjectSpec
import modulecheck.testing.DynamicTests
import modulecheck.testing.tempDir
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.io.File

val DEFAULT_GRADLE_VERSION: String = System.getProperty("modulecheck.gradleVersion", "6.8.3")

interface GradleRunnerAware {
  val gradleRunner: GradleRunner
  fun build(vararg tasks: String): BuildResult {
    return gradleRunner.withArguments(*tasks).build()
  }
}

abstract class BaseTest : HermitJUnit5(),
                          DynamicTests,
                          GradleRunnerAware {

  val testProjectDir by tempDir()

  fun File.relativePath() = path.removePrefix(testProjectDir.path)

  fun String.fixPath(): String = replace(File.separator, "/")

  private val kotlinVersions = listOf("1.5.0-M1", "1.4.31", "1.3.72")
  private val gradleVersions = listOf("7.0-milestone-3", "6.8.3")
  private val agpVersions = listOf("4.1.3", "4.0.1")

  val optionPermutations = kotlinVersions.flatMap { kv ->
    gradleVersions.flatMap { gv ->
      agpVersions.map { av ->
        TestOptions(kv, gv, av)
      }
    }
  }

  data class TestOptions(
    val kotlinVersion: String, val gradleVersion: String, val agpVersion: String
  ) {
    override fun toString(): String {
      return "$kotlinVersion - $gradleVersion - $agpVersion"
    }
  }

  private val gradleVersion =
    System.getProperty("modulecheck.gradleVersion", DEFAULT_GRADLE_VERSION)

  override val gradleRunner by resets {
    GradleRunner
      .create()
      // .forwardOutput()
      .withGradleVersion(gradleVersion)
      .withPluginClasspath()
      // .withDebug(true)
      .withProjectDir(testProjectDir)
  }

  private var testInfo: TestInfo? = null

  fun test(
    projectSpecFactory: () -> ProjectSpec,
    action: GradleRunnerAware.() -> Unit
  ) = test(projectSpecFactory(), action)

  fun test(
    projectSpec: ProjectSpec,
    action: GradleRunnerAware.() -> Unit
  ) = optionPermutations.map {
    {
      projectSpec.edit {
        projectBuildSpec?.edit {
          kotlinVersion = it.kotlinVersion
          agpVersion = it.agpVersion
        }
      }.writeIn(testProjectDir.toPath())

      val runner = GradleRunner
        .create()
        // .forwardOutput()
        .withGradleVersion(it.gradleVersion)
        .withPluginClasspath()
        // .withDebug(true)
        .withProjectDir(testProjectDir)

      object : GradleRunnerAware {
        override val gradleRunner = runner

        override fun toString(): String {
          return it.toString()
        }
      }
    }
  }.dynamic({ testInfo!!.displayName.replace("()", "") }, action)

  fun BuildResult.shouldSucceed() {
    tasks.forEach { it.outcome shouldBe TaskOutcome.SUCCESS }
  }

  fun shouldFailWithMessage(vararg tasks: String, messageBlock: (String) -> Unit) {
    val result = gradleRunner.withArguments(*tasks).buildAndFail()

    result.tasks.map { it.outcome } shouldContain TaskOutcome.FAILED
    messageBlock(result.output.fixPath())
  }

  @BeforeEach
  fun beforeEach(testInfo: TestInfo) {
    this.testInfo = testInfo
  }

  @AfterEach
  fun afterEach() {
    testInfo = null
  }
}
