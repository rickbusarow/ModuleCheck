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

import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.gradle.asConfigurationName
import modulecheck.runtime.test.ProjectFindingReport.unusedKaptPlugin
import modulecheck.runtime.test.ProjectFindingReport.unusedKaptProcessor
import modulecheck.runtime.test.RunnerTest
import org.junit.jupiter.api.Test

class UnusedKaptProcessorTest : RunnerTest() {

  val dagger = "com.google.dagger:dagger-compiler:2.40.5"

  @Test
  fun `unused from kapt configuration without autoCorrect should fail`() {

    val app = kotlinProject(":app") {
      hasKapt = true

      addExternalDependency(ConfigurationName.kapt, dagger)

      buildFile {
        """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
        }

        dependencies {
          kapt("$dagger")
        }
        """
      }
    }

    run(
      autoCorrect = false
    ).isSuccess shouldBe false

    app.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
        }

        dependencies {
          kapt("$dagger")
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":app" to listOf(
        unusedKaptProcessor(
          fixed = false,
          configuration = "kapt",
          dependency = "com.google.dagger:dagger-compiler",
          position = "7, 3"
        ),
        unusedKaptPlugin(
          fixed = false,
          dependency = "org.jetbrains.kotlin.kapt",
          position = "3, 3"
        )
      )
    )
  }

  @Test
  fun `unused from kapt configuration with alternate plugin id without autoCorrect should fail`() {

    val app = kotlinProject(":app") {
      hasKapt = true

      addExternalDependency(ConfigurationName.kapt, dagger)

      buildFile {
        """
        plugins {
          id("kotlin-jvm")
          id("kotlin-kapt")
        }

        dependencies {
          kapt("$dagger")
        }
        """
      }
    }

    run(
      autoCorrect = false
    ).isSuccess shouldBe false

    app.buildFile shouldHaveText """
        plugins {
          id("kotlin-jvm")
          id("kotlin-kapt")
        }

        dependencies {
          kapt("$dagger")
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":app" to listOf(
        unusedKaptProcessor(
          fixed = false,
          configuration = "kapt",
          dependency = "com.google.dagger:dagger-compiler",
          position = "7, 3"
        ),
        unusedKaptPlugin(
          fixed = false,
          dependency = "org.jetbrains.kotlin.kapt",
          position = "3, 3"
        )
      )
    )
  }

  @Test
  fun `unused from non-kapt configuration without autoCorrect should pass without changes`() {

    val app = kotlinProject(":app") {
      hasKapt = true

      addExternalDependency(ConfigurationName.api, dagger)

      buildFile {
        """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
        }

        dependencies {
          api("$dagger")
        }
        """
      }
    }

    run(
      autoCorrect = false
    ).isSuccess shouldBe true

    app.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
        }

        dependencies {
          api("$dagger")
        }
    """

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `used in main with main kapt should pass without changes`() {

    val app = kotlinProject(":app") {
      hasKapt = true

      addExternalDependency(ConfigurationName.kapt, dagger)

      buildFile {
        """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
        }

        dependencies {
          kapt("$dagger")
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.app

        import javax.inject.Inject

        class App @Inject constructor()
        """
      )
    }

    run(
      autoCorrect = false
    ).isSuccess shouldBe true

    app.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
        }

        dependencies {
          kapt("$dagger")
        }
    """

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `used in test with test kapt should pass without changes`() {

    val app = kotlinProject(":app") {
      hasKapt = true

      addExternalDependency("kaptTest".asConfigurationName(), dagger)

      buildFile {
        """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
        }

        dependencies {
          kaptTest("$dagger")
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.app

        import javax.inject.Inject

        class App @Inject constructor()
        """,
        SourceSetName.TEST
      )
    }

    run(
      autoCorrect = false
    ).isSuccess shouldBe true

    app.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
        }

        dependencies {
          kaptTest("$dagger")
        }
    """

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `unused with main kapt with autoCorrect and no other processors should remove processor and plugin`() {

    val app = kotlinProject(":app") {
      hasKapt = true

      addExternalDependency(ConfigurationName.kapt, dagger)

      buildFile {
        """
        plugins {
          kotlin("jvm")
          kotlin("kapt")
        }

        dependencies {
          kapt("$dagger")
        }
        """
      }
    }

    run().isSuccess shouldBe true

    app.buildFile shouldHaveText """
      plugins {
        kotlin("jvm")
        // kotlin("kapt")  // ModuleCheck finding [unused-kapt-plugin]
      }

      dependencies {
        // kapt("com.google.dagger:dagger-compiler:2.40.5")  // ModuleCheck finding [unused-kapt-processor]
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":app" to listOf(
        unusedKaptProcessor(
          fixed = true,
          configuration = "kapt",
          dependency = "com.google.dagger:dagger-compiler",
          position = "7, 3"
        ),
        unusedKaptPlugin(
          fixed = true,
          dependency = "org.jetbrains.kotlin.kapt",
          position = "3, 3"
        )
      )
    )
  }

  @Test
  fun `unused with main kapt with alternate plugin id with autoCorrect and no other processors should remove processor and plugin`() {

    val app = kotlinProject(":app") {
      hasKapt = true

      addExternalDependency(ConfigurationName.kapt, dagger)

      buildFile {
        """
        plugins {
          id("kotlin-jvm")
          id("kotlin-kapt")
        }

        dependencies {
          kapt("$dagger")
        }
        """
      }
    }

    run().isSuccess shouldBe true

    app.buildFile shouldHaveText """
      plugins {
        id("kotlin-jvm")
        // id("kotlin-kapt")  // ModuleCheck finding [unused-kapt-plugin]
      }

      dependencies {
        // kapt("com.google.dagger:dagger-compiler:2.40.5")  // ModuleCheck finding [unused-kapt-processor]
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":app" to listOf(
        unusedKaptProcessor(
          fixed = true,
          configuration = "kapt",
          dependency = "com.google.dagger:dagger-compiler",
          position = "7, 3"
        ),
        unusedKaptPlugin(
          fixed = true,
          dependency = "org.jetbrains.kotlin.kapt",
          position = "3, 3"
        )
      )
    )
  }
}
