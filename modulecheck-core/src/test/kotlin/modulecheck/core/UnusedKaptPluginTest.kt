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

import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.runtime.test.ProjectFindingReport.unusedKaptPlugin
import modulecheck.runtime.test.RunnerTest
import org.junit.jupiter.api.Test

class UnusedKaptPluginTest : RunnerTest() {

  val dagger = "com.google.dagger:dagger-compiler:2.40.5"

  @Test
  fun `plugin applied but with processor in non-kapt configuration without autoCorrect should remove plugin`() {

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

    run().isSuccess shouldBe true

    app.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
          // kotlin("kapt")  // ModuleCheck finding [unused-kapt-plugin]
        }

        dependencies {
          api("$dagger")
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":app" to listOf(
        unusedKaptPlugin(
          fixed = true,
          dependency = "org.jetbrains.kotlin.kapt",
          position = "3, 3"
        )
      )
    )
  }

  @Test
  fun `unused with main kapt no other processors should remove plugin`() {

    val app = kotlinProject(":app") {
      hasKapt = true

      buildFile {
        """
        plugins {
          id("kotlin-jvm")
          id("kotlin-kapt")
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
    """

    logger.parsedReport() shouldBe listOf(
      ":app" to listOf(
        unusedKaptPlugin(
          fixed = true,
          dependency = "org.jetbrains.kotlin.kapt",
          position = "3, 3"
        )
      )
    )
  }
}
