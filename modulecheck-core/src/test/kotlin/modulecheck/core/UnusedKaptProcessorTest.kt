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

package modulecheck.core

import modulecheck.core.rule.ModuleCheckRuleFactory
import modulecheck.core.rule.MultiRuleFindingFactory
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.gradle.asConfigurationName
import modulecheck.runtime.test.RunnerTest
import org.junit.jupiter.api.Test

class UnusedKaptProcessorTest : RunnerTest() {

  val ruleFactory by resets { ModuleCheckRuleFactory() }

  val findingFactory by resets {
    MultiRuleFindingFactory(
      settings,
      ruleFactory.create(settings)
    )
  }

  val dagger = "com.google.dagger:dagger-compiler:2.40.5"

  @Test
  fun `unused from kapt configuration without autoCorrect should fail`() {

    val runner = runner(
      autoCorrect = false,
      findingFactory = findingFactory
    )

    val app = project(":app") {
      hasKapt = true

      addExternalDependency(ConfigurationName.kapt, dagger)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
        }

        dependencies {
          kapt("$dagger")
        }
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe false

    app.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
        }

        dependencies {
          kapt("$dagger")
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :app
                   dependency                           name                          source    build file
                X  org.jetbrains.kotlin.kapt            unusedKaptPlugin                        /app/build.gradle.kts: (3, 3):
                X  com.google.dagger:dagger-compiler    unusedKaptProcessor (kapt)              /app/build.gradle.kts: (7, 3):

        ModuleCheck found 2 issues
        """
  }

  @Test
  fun `unused from non-kapt configuration without autoCorrect should pass without changes`() {

    val runner = runner(
      autoCorrect = false,
      findingFactory = findingFactory
    )

    val app = project(":app") {
      hasKapt = true

      addExternalDependency(ConfigurationName.api, dagger)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
        }

        dependencies {
          api("$dagger")
        }
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    app.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
        }

        dependencies {
          api("$dagger")
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe "ModuleCheck found 0 issues"
  }

  @Test
  fun `used in main with main kapt should pass without changes`() {

    val runner = runner(
      autoCorrect = false,
      findingFactory = findingFactory
    )

    val app = project(":app") {
      hasKapt = true

      addExternalDependency(ConfigurationName.kapt, dagger)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
        }

        dependencies {
          kapt("$dagger")
        }
        """
      )
      addSource(
        "com/modulecheck/app/App.kt",
        """
        package com.modulecheck.app

        import javax.inject.Inject

        class App @Inject constructor()
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    app.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
        }

        dependencies {
          kapt("$dagger")
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe "ModuleCheck found 0 issues"
  }

  @Test
  fun `used in test with test kapt should pass without changes`() {

    val runner = runner(
      autoCorrect = false,
      findingFactory = findingFactory
    )

    val app = project(":app") {
      hasKapt = true

      addExternalDependency("kaptTest".asConfigurationName(), dagger)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
        }

        dependencies {
          kaptTest("$dagger")
        }
        """
      )
      addSource(
        "com/modulecheck/app/App.kt",
        """
        package com.modulecheck.app

        import javax.inject.Inject

        class App @Inject constructor()
        """,
        SourceSetName.TEST
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    app.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
        }

        dependencies {
          kaptTest("$dagger")
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe "ModuleCheck found 0 issues"
  }

  @Test
  fun `unused with main kapt with autoCorrect and no other processors should remove processor and plugin`() {

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val app = project(":app") {
      hasKapt = true

      addExternalDependency(ConfigurationName.kapt, dagger)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
        }

        dependencies {
          kapt("$dagger")
        }
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    app.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
          // kotlin("kapt")  // ModuleCheck finding [unusedKaptPlugin]
        }

        dependencies {
          // kapt("com.google.dagger:dagger-compiler:2.40.5")  // ModuleCheck finding [unusedKaptProcessor (kapt)]
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
          :app
                 dependency                           name                          source    build file
              ✔  org.jetbrains.kotlin.kapt            unusedKaptPlugin                        /app/build.gradle.kts: (3, 3):
              ✔  com.google.dagger:dagger-compiler    unusedKaptProcessor (kapt)              /app/build.gradle.kts: (7, 3):

      ModuleCheck found 2 issues
      """
  }
}
