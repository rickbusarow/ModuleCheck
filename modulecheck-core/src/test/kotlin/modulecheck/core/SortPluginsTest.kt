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

package modulecheck.core

import modulecheck.api.test.TestChecksSettings
import modulecheck.api.test.TestSettings
import modulecheck.core.rule.ModuleCheckRuleFactory
import modulecheck.core.rule.MultiRuleFindingFactory
import modulecheck.runtime.test.ProjectFindingReport.unsortedPlugins
import modulecheck.runtime.test.RunnerTest
import modulecheck.testing.writeGroovy
import modulecheck.testing.writeKotlin
import org.junit.jupiter.api.Test
import java.io.File

class SortPluginsTest : RunnerTest() {

  val ruleFactory by resets { ModuleCheckRuleFactory() }

  override val settings by resets { TestSettings(checks = TestChecksSettings(sortPlugins = true)) }
  val findingFactory by resets {
    MultiRuleFindingFactory(
      settings,
      ruleFactory.create(settings)
    )
  }

  @Test
  fun `kts out-of-order plugins should be sorted`() {

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      buildFile.writeKotlin(
        """
      plugins {
        id("io.gitlab.arturbosch.detekt") version "1.15.0"
        javaLibrary
        kotlin("jvm")
      }
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

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
  fun `kts sorting should be idempotent`() {

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      buildFile.writeKotlin(
        """
      plugins {
        id("io.gitlab.arturbosch.detekt") version "1.15.0"
        javaLibrary
        kotlin("jvm")
      }
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

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

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        kotlin("jvm")
        javaLibrary
        id("io.gitlab.arturbosch.detekt") version "1.15.0"
      }
    """

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `groovy out-of-order plugins should be sorted`() {

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
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

    runner.run(allProjects()).isSuccess shouldBe true

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
  fun `groovy sorting should be idempotent`() {

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
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

    runner.run(allProjects()).isSuccess shouldBe true

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

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        javaLibrary
        id 'io.gitlab.arturbosch.detekt' version '1.15.0'
        id 'org.jetbrains.kotlin.jvm'
      }
    """

    logger.parsedReport() shouldBe listOf()
  }
}
