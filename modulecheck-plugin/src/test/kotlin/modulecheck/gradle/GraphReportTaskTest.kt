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

import modulecheck.project.test.createSafely
import modulecheck.specs.DEFAULT_KOTLIN_VERSION
import modulecheck.utils.child
import org.junit.jupiter.api.Test

internal class GraphReportTaskTest : BasePluginTest() {

  @Test
  fun `graphs report should be created if graph task is invoked with default settings`() {

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

    shouldSucceed("moduleCheckGraphs")

    root.projectDir.child(
      "app", "build", "reports", "modulecheck", "graphs", "main.dot"
    ).readText() shouldBe """
      strict digraph DependencyGraph {
        ratio=0.5;
        node [style="rounded,filled" shape=box];

        ":app" [fillcolor = "#F89820"];
        ":lib1" [fillcolor = "#F89820"];
        ":lib2" [fillcolor = "#F89820"];

        ":app" -> ":lib1" [style = bold; color = "#007744"];
        ":app" -> ":lib2" [style = bold; color = "#007744"];

        ":lib2" -> ":lib1" [style = bold; color = "#007744"];

        {rank = same; ":lib1";}
        {rank = same; ":lib2";}
        {rank = same; ":app";}
      }
      """
  }
}
