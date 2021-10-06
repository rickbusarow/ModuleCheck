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

import io.kotest.matchers.collections.shouldContain
import modulecheck.specs.DEFAULT_AGP_VERSION
import modulecheck.specs.DEFAULT_GRADLE_VERSION
import modulecheck.specs.DEFAULT_KOTLIN_VERSION
import modulecheck.testing.BaseTest
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.TestInfo
import kotlin.properties.Delegates

abstract class BasePluginTest : BaseTest() {

  private val kotlinVersion = DEFAULT_KOTLIN_VERSION
  private val agpVersion = DEFAULT_AGP_VERSION
  private val gradleVersion = DEFAULT_GRADLE_VERSION

  val gradleRunner by resets {
    GradleRunner.create()
      .forwardOutput()
      .withGradleVersion(gradleVersion)
      .withPluginClasspath()
      // .withDebug(true)
      .withProjectDir(testProjectDir)
  }

  private var testInfo: TestInfo by Delegates.notNull()

  fun build(vararg tasks: String): BuildResult {
    return gradleRunner.withArguments(*tasks).build()
  }

  fun BuildResult.shouldSucceed() {
    tasks.forEach { it.outcome shouldBe TaskOutcome.SUCCESS }
  }

  fun shouldFailWithMessage(vararg tasks: String, messageBlock: (String) -> Unit) {
    val result = gradleRunner.withArguments(*tasks).buildAndFail()

    result.tasks.map { it.outcome } shouldContain TaskOutcome.FAILED
    messageBlock(result.output.fixPath())
  }
}
