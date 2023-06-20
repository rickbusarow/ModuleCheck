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

package modulecheck.core

import modulecheck.config.fake.TestChecksSettings
import modulecheck.config.fake.TestSettings
import modulecheck.runtime.test.ProjectFindingReport.unsortedPlugins
import modulecheck.runtime.test.RunnerTest
import modulecheck.runtime.test.RunnerTestEnvironment
import modulecheck.testing.writeGroovy
import org.junit.jupiter.api.Test
import java.io.File

class SortPluginsTest : RunnerTest() {

  override val settings: RunnerTestEnvironment.() -> TestSettings = {
    TestSettings(checks = TestChecksSettings(sortPlugins = true))
  }

  @Test
  fun `kts out-of-order plugins should be sorted`() = test {

    val lib1 = kotlinProject(":lib1") {
      buildFile {
        """
      plugins {
        id("io.gitlab.arturbosch.detekt") version "1.15.0"
        javaLibrary
        kotlin("jvm")
      }
        """
      }
    }

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        kotlin("jvm")
        javaLibrary
        id("io.gitlab.arturbosch.detekt") version "1.15.0"
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(unsortedPlugins(fixed = true))
    )
  }

  @Test
  fun `kts sorting should be idempotent`() = test {

    val lib1 = kotlinProject(":lib1") {
      buildFile {
        """
      plugins {
        id("io.gitlab.arturbosch.detekt") version "1.15.0"
        javaLibrary
        kotlin("jvm")
      }
        """
      }
    }

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        kotlin("jvm")
        javaLibrary
        id("io.gitlab.arturbosch.detekt") version "1.15.0"
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(unsortedPlugins(fixed = true))
    )
    logger.clear()

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        kotlin("jvm")
        javaLibrary
        id("io.gitlab.arturbosch.detekt") version "1.15.0"
      }
    """

    logger.parsedReport() shouldBe emptyList()
  }

  @Test
  fun `groovy out-of-order plugins should be sorted`() = test {

    val lib1 = kotlinProject(":lib1") {
      buildFile.delete()
      buildFile = File(projectDir, "build.gradle")
      buildFile.writeGroovy(
        """
      plugins {
        id 'io.gitlab.arturbosch.detekt' version '1.15.0'
        javaLibrary
        id 'org.jetbrains.kotlin.jvm'
      }
        """
      )
    }

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        javaLibrary
        id 'io.gitlab.arturbosch.detekt' version '1.15.0'
        id 'org.jetbrains.kotlin.jvm'
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(unsortedPlugins(fixed = true))
    )
  }

  @Test
  fun `groovy sorting should be idempotent`() = test {

    val lib1 = kotlinProject(":lib1") {
      buildFile.delete()
      buildFile = File(projectDir, "build.gradle")
      buildFile.writeGroovy(
        """
      plugins {
        id 'io.gitlab.arturbosch.detekt' version '1.15.0'
        javaLibrary
        id 'org.jetbrains.kotlin.jvm'
      }
        """
      )
    }

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        javaLibrary
        id 'io.gitlab.arturbosch.detekt' version '1.15.0'
        id 'org.jetbrains.kotlin.jvm'
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(unsortedPlugins(fixed = true))
    )
    logger.clear()

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        javaLibrary
        id 'io.gitlab.arturbosch.detekt' version '1.15.0'
        id 'org.jetbrains.kotlin.jvm'
      }
    """

    logger.parsedReport() shouldBe emptyList()
  }
}
