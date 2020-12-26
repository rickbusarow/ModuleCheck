/*
 * Copyright (C) 2020 Rick Busarow
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

package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.testing.ProjectBuildSpec
import com.rickbusarow.modulecheck.testing.ProjectSettingsSpec
import com.rickbusarow.modulecheck.testing.ProjectSpec
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File

inline fun <T : Any, E> T.applyEach(elements: Iterable<E>, block: T.(E) -> Unit): T {
  elements.forEach { element -> this.block(element) }
  return this
}

class SortDependenciesTest : FreeSpec({

  val testProjectDir = tempDir()

  fun File.relativePath() = path.removePrefix(testProjectDir.path)

  "test" - {

    val projects = List(10) {
      ProjectSpec.Builder("lib-$it")
        .build()
    }

    val psBuilder = ProjectSpec.Builder("project")
      .addSettingsSpec(
        ProjectSettingsSpec.Builder()
          .applyEach(projects) { project ->
            addInclude(project.gradlePath)
          }
          .addInclude("app")
          .build()
      )
      .addBuildSpec(
        ProjectBuildSpec.Builder()
          .addPlugin("id(\"com.rickbusarow.module-check\")")
          .buildScript()
          .build()
      )
      .applyEach(projects) { project ->
        addSubproject(project)
      }

    "configurations should be grouped and sorted" {

      psBuilder
        .addSubproject(
          ProjectSpec.Builder("app")
            .addBuildSpec(
              ProjectBuildSpec.Builder()
                .addPlugin("kotlin(\"jvm\")")
                .addProjectDependency("runtimeOnly", "lib-1")
                .addProjectDependency("api", "lib-3")
                .addProjectDependency("implementation", "lib-7")
                .addProjectDependency("compileOnly", "lib-4")
                .addProjectDependency("api", "lib-0")
                .addProjectDependency("testImplementation", "lib-5")
                .addProjectDependency("compileOnly", "lib-6")
                .addProjectDependency("implementation", "lib-2")
                .addProjectDependency("testImplementation", "lib-8")
                .addProjectDependency("implementation", "lib-9")
                .build()
            )
            .build()
        )

      psBuilder
        .build()
        .writeIn(testProjectDir.toPath())

      val result = GradleRunner.create()
        .withPluginClasspath()
        .withDebug(true)
        .withProjectDir(testProjectDir)
        .withArguments("moduleCheckSortDependencies")
        .build()

      result.task(":moduleCheckSortDependencies")?.outcome shouldBe TaskOutcome.SUCCESS

      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  api(project(path = ":lib-0"))
        |  api(project(path = ":lib-3"))
        |
        |  compileOnly(project(path = ":lib-4"))
        |  compileOnly(project(path = ":lib-6"))
        |
        |  implementation(project(path = ":lib-2"))
        |  implementation(project(path = ":lib-7"))
        |  implementation(project(path = ":lib-9"))
        |
        |  runtimeOnly(project(path = ":lib-1"))
        |
        |  testImplementation(project(path = ":lib-5"))
        |  testImplementation(project(path = ":lib-8"))
        |}
        |""".trimMargin()
    }

    "external dependencies should be grouped separately" {

      psBuilder
        .addSubproject(
          ProjectSpec.Builder("app")
            .addBuildSpec(
              ProjectBuildSpec.Builder()
                .addPlugin("kotlin(\"jvm\")")
                .addProjectDependency("runtimeOnly", "lib-1")
                .addExternalDependency("api", "com.squareup:kotlinpoet:1.7.2")
                .addProjectDependency("api", "lib-3")
                .addProjectDependency("implementation", "lib-7")
                .addExternalDependency(
                  "implementation",
                  "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2"
                )
                .addProjectDependency("compileOnly", "lib-4")
                .addProjectDependency("api", "lib-0")
                .addProjectDependency("testImplementation", "lib-5")
                .addProjectDependency("compileOnly", "lib-6")
                .addProjectDependency("implementation", "lib-2")
                .addProjectDependency("testImplementation", "lib-8")
                .addExternalDependency(
                  "testImplementation",
                  "org.junit.jupiter:junit-jupiter-api:5.7.0"
                )
                .addProjectDependency("implementation", "lib-9")
                .build()
            )
            .build()
        )

      psBuilder
        .build()
        .writeIn(testProjectDir.toPath())

      val result = GradleRunner.create()
        .withPluginClasspath()
        .withDebug(true)
        .withProjectDir(testProjectDir)
        .withArguments("moduleCheckSortDependencies")
        .build()

      result.task(":moduleCheckSortDependencies")?.outcome shouldBe TaskOutcome.SUCCESS

      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  api(":com.squareup:kotlinpoet:1.7.2")
        |
        |  api(project(path = ":lib-0"))
        |  api(project(path = ":lib-3"))
        |
        |  compileOnly(project(path = ":lib-4"))
        |  compileOnly(project(path = ":lib-6"))
        |
        |  implementation(":org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
        |
        |  implementation(project(path = ":lib-2"))
        |  implementation(project(path = ":lib-7"))
        |  implementation(project(path = ":lib-9"))
        |
        |  runtimeOnly(project(path = ":lib-1"))
        |
        |  testImplementation(":org.junit.jupiter:junit-jupiter-api:5.7.0")
        |
        |  testImplementation(project(path = ":lib-5"))
        |  testImplementation(project(path = ":lib-8"))
        |}
        |""".trimMargin()
    }

    "comments above" {

      psBuilder
        .addSubproject(
          ProjectSpec.Builder("app")
            .addBuildSpec(
              ProjectBuildSpec.Builder()
                .addPlugin("kotlin(\"jvm\")")
                .addProjectDependency("runtimeOnly", "lib-1")
                .addExternalDependency("api", "com.squareup:kotlinpoet:1.7.2")
                .addProjectDependency("api", "lib-3")
                .addProjectDependency("implementation", "lib-7")
                .addExternalDependency(
                  "implementation",
                  "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2"
                )
                .addProjectDependency("compileOnly", "lib-4")
                .addProjectDependency("api", "lib-0")
                .addProjectDependency("testImplementation", "lib-5")
                .addProjectDependency("compileOnly", "lib-6")
                .addProjectDependency("implementation", "lib-2", "//foo\n  ")
                .addProjectDependency("testImplementation", "lib-8")
                .addExternalDependency(
                  "testImplementation",
                  "org.junit.jupiter:junit-jupiter-api:5.7.0"
                )
                .addProjectDependency("implementation", "lib-9", "//foo\n  ")
                .build()
            )
            .build()
        )

      psBuilder
        .build()
        .writeIn(testProjectDir.toPath())

      val result = GradleRunner.create()
        .withPluginClasspath()
        .withDebug(true)
        .withProjectDir(testProjectDir)
        .withArguments("moduleCheckSortDependencies")
        .build()

      result.task(":moduleCheckSortDependencies")?.outcome shouldBe TaskOutcome.SUCCESS

      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  api(":com.squareup:kotlinpoet:1.7.2")
        |
        |  api(project(path = ":lib-0"))
        |  api(project(path = ":lib-3"))
        |
        |  compileOnly(project(path = ":lib-4"))
        |  compileOnly(project(path = ":lib-6"))
        |
        |  implementation(":org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
        |
        |  implementation(project(path = ":lib-2"))
        |  implementation(project(path = ":lib-7"))
        |  implementation(project(path = ":lib-9"))
        |
        |  runtimeOnly(project(path = ":lib-1"))
        |
        |  testImplementation(":org.junit.jupiter:junit-jupiter-api:5.7.0")
        |
        |  testImplementation(project(path = ":lib-5"))
        |  testImplementation(project(path = ":lib-8"))
        |}
        |""".trimMargin()
    }
  }
})
