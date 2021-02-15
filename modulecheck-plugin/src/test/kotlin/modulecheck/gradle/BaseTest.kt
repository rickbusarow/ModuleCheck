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
import modulecheck.specs.DEFAULT_AGP_VERSION
import modulecheck.specs.DEFAULT_KOTLIN_VERSION
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo

public val DEFAULT_GRADLE_VERSION: String = System.getProperty("modulecheck.gradleVersion", "6.8.2")

abstract class BaseTest : HermitJUnit5() {

  val testProjectDir by tempDir("___${System.currentTimeMillis()}")

  private val kotlinVersion =
    System.getProperty("modulecheck.kotlinVersion", DEFAULT_KOTLIN_VERSION)
  private val agpVersion = System.getProperty("modulecheck.agpVersion", DEFAULT_AGP_VERSION)
  private val gradleVersion =
    System.getProperty("modulecheck.gradleVersion", DEFAULT_GRADLE_VERSION)

  val gradleRunner by resets {
    GradleRunner
      .create()
      .forwardOutput()
      .withGradleVersion(gradleVersion)
      .withPluginClasspath()
      .withDebug(true)
      .withProjectDir(testProjectDir)
  }

  private var testInfo: TestInfo? = null

  @BeforeEach
  fun beforeEach(testInfo: TestInfo) {
    this.testInfo = testInfo
  }

  @AfterEach
  fun afterEach() {
    testInfo = null
  }
}
