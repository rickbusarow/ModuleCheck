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

import modulecheck.specs.DEFAULT_KOTLIN_VERSION
import modulecheck.testing.createSafely
import modulecheck.utils.child
import org.junit.jupiter.api.Test

internal class DepthReportTaskTest : BasePluginTest() {

  @Test
  fun `depth report should be created if depth task is invoked with default settings`() {

    val root = project(":") {

      buildFile.writeText(
        """
        plugins {
          id("com.rickbusarow.module-check")
        }
        """.trimIndent()
      )

      projectDir.child("settings.gradle.kts")
        .createSafely(
          """
          pluginManagement {
            repositories {
              gradlePluginPortal()
              mavenCentral()
              google()
            }
            resolutionStrategy {
              eachPlugin {
                if (requested.id.id.startsWith("org.jetbrains.kotlin")) {
                  useVersion("$DEFAULT_KOTLIN_VERSION")
                }
              }
            }
          }
          dependencyResolutionManagement {
            @Suppress("UnstableApiUsage")
            repositories {
              google()
              mavenCentral()
            }
          }
          include(":lib1")
          include(":lib2")
          include(":app")
          """.trimIndent()
        )

      project(":lib1") {
        buildFile.writeText(
          """
          plugins {
            kotlin("jvm")
          }
          """.trimIndent()
        )
      }

      project(":lib2") {
        buildFile.writeText(
          """
          plugins {
            kotlin("jvm")
          }
          dependencies {
            implementation(project(":lib1"))
          }
          """.trimIndent()
        )
      }

      project(":app") {
        buildFile.writeText(
          """
          plugins {
            kotlin("jvm")
          }
          dependencies {
            implementation(project(":lib1"))
            implementation(project(":lib2"))
          }
          """.trimIndent()
        )
      }
    }

    shouldSucceed("moduleCheckDepths")

    root.projectDir.child(
      "build", "reports", "modulecheck", "depths.txt"
    ).readText() shouldBe """
      -- ModuleCheck Depth results --

      :app
          source set      depth    most expensive dependencies
          main            2        [:lib2]

      :lib2
          source set      depth    most expensive dependencies
          main            1        [:lib1]

      """
  }
}
