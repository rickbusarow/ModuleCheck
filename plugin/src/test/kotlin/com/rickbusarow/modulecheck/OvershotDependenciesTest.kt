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

package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.internal.applyEach
import com.rickbusarow.modulecheck.specs.ProjectBuildSpec
import com.rickbusarow.modulecheck.specs.ProjectSettingsSpec
import com.rickbusarow.modulecheck.specs.ProjectSpec
import com.rickbusarow.modulecheck.specs.ProjectSrcSpec
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Path

class OvershotDependenciesTest : FreeSpec({

  val testProjectDir = tempDir()

  fun File.relativePath() = path.removePrefix(testProjectDir.path)

  val projects = List(10) {
    ProjectSpec.builder("lib-$it")
      .build()
  }

  val projectSpecBuilder = ProjectSpec.builder("project")
    .addSettingsSpec(
      ProjectSettingsSpec.builder()
        .applyEach(projects) { project ->
          addInclude(project.gradlePath)
        }
        .addInclude("app")
        .build()
    )
    .addBuildSpec(
      ProjectBuildSpec.builder()
        .addPlugin("id(\"com.rickbusarow.module-check\")")
        .buildScript()
        .build()
    )
    .applyEach(projects) { project ->
      addSubproject(project)
    }

  "overshot dependencies should be added" {

    projectSpecBuilder
      .addSubproject(
        ProjectSpec.builder("app")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .addProjectDependency("api", "lib-3")
              .build()
          )
          .addSrcSpec(
            ProjectSrcSpec.builder(Path.of("src/main/kotlin"))
              .addFile(
                FileSpec.builder("com.example.app", "MyApp.kt")
                  .addImport("com.example.lib1", "Lib1Class")
                  .addImport("com.example.lib2", "Lib2Class")
                  .build()
              )
              .build()
          )
          .build()
      )
      .addSubproject(
        ProjectSpec.builder("lib-1")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .build()
          )
          .addSrcSpec(
            ProjectSrcSpec.builder(Path.of("src/main/kotlin"))
              .addFile(
                FileSpec.builder("com.example.lib1", "Lib1Class.kt")
                  .addType(TypeSpec.classBuilder("Lib1Class").build())
                  .build()
              )
              .build()
          )
          .build()
      )
      .addSubproject(
        ProjectSpec.builder("lib-2")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .build()
          )
          .addSrcSpec(
            ProjectSrcSpec.builder(Path.of("src/main/kotlin"))
              .addFile(
                FileSpec.builder("com.example.lib2", "Lib2Class.kt")
                  .addType(TypeSpec.classBuilder("Lib2Class").build())
                  .build()
              )
              .build()
          )
          .build()
      )
      .addSubproject(
        ProjectSpec.builder("lib-3")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .addProjectDependency("api", "lib-1")
              .addProjectDependency("api", "lib-2")
              .build()
          )
          .build()
      )
      .build()
      .writeIn(testProjectDir.toPath())

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withDebug(true)
      .withProjectDir(testProjectDir)
      .withArguments("moduleCheckOvershot")
      .build()

    result.task(":moduleCheckOvershot")!!.outcome shouldBe TaskOutcome.SUCCESS

    File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  api(project(path = ":lib-1"))
        |  api(project(path = ":lib-2"))
        |  api(project(path = ":lib-3"))
        |}
        |""".trimMargin()
  }

  "added dependencies should match the configuration of the dependency which provided them" {

    projectSpecBuilder
      .addSubproject(
        ProjectSpec.builder("app")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .addProjectDependency("implementation", "lib-3")
              .build()
          )
          .addSrcSpec(
            ProjectSrcSpec.builder(Path.of("src/main/kotlin"))
              .addFile(
                FileSpec.builder("com.example.app", "MyApp.kt")
                  .addImport("com.example.lib1", "Lib1Class")
                  .addImport("com.example.lib2", "Lib2Class")
                  .build()
              )
              .build()
          )
          .build()
      )
      .addSubproject(
        ProjectSpec.builder("lib-1")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .build()
          )
          .addSrcSpec(
            ProjectSrcSpec.builder(Path.of("src/main/kotlin"))
              .addFile(
                FileSpec.builder("com.example.lib1", "Lib1Class.kt")
                  .addType(TypeSpec.classBuilder("Lib1Class").build())
                  .build()
              )
              .build()
          )
          .build()
      )
      .addSubproject(
        ProjectSpec.builder("lib-2")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .build()
          )
          .addSrcSpec(
            ProjectSrcSpec.builder(Path.of("src/main/kotlin"))
              .addFile(
                FileSpec.builder("com.example.lib2", "Lib2Class.kt")
                  .addType(TypeSpec.classBuilder("Lib2Class").build())
                  .build()
              )
              .build()
          )
          .build()
      )
      .addSubproject(
        ProjectSpec.builder("lib-3")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .addProjectDependency("api", "lib-1")
              .addProjectDependency("api", "lib-2")
              .build()
          )
          .build()
      )
      .build()
      .writeIn(testProjectDir.toPath())

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withDebug(true)
      .withProjectDir(testProjectDir)
      .withArguments("moduleCheckOvershot")
      .build()

    result.task(":moduleCheckOvershot")!!.outcome shouldBe TaskOutcome.SUCCESS

    // lib-1 and lib-2 are api dependencies in lib-3,
    // but lib-3 is an implementation dependency of app
    // so here, they're added as implementation dependencies
    File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  implementation(project(path = ":lib-1"))
        |  implementation(project(path = ":lib-2"))
        |  implementation(project(path = ":lib-3"))
        |}
        |""".trimMargin()
  }

  "dependencies should not be overshot if providing dependency is used" {

    projectSpecBuilder
      .addSubproject(
        ProjectSpec.builder("app")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .addProjectDependency("api", "lib-3")
              .build()
          )
          .addSrcSpec(
            ProjectSrcSpec.builder(Path.of("src/main/kotlin"))
              .addFile(
                FileSpec.builder("com.example.app", "MyApp.kt")
                  .addImport("com.example.lib1", "Lib1Class")
                  .addImport("com.example.lib2", "Lib2Class")
                  .addImport("com.example.lib3", "Lib3Class")
                  .build()
              )
              .build()
          )
          .build()
      )
      .addSubproject(
        ProjectSpec.builder("lib-1")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .build()
          )
          .addSrcSpec(
            ProjectSrcSpec.builder(Path.of("src/main/kotlin"))
              .addFile(
                FileSpec.builder("com.example.lib1", "Lib1Class.kt")
                  .addType(TypeSpec.classBuilder("Lib1Class").build())
                  .build()
              )
              .build()
          )
          .build()
      )
      .addSubproject(
        ProjectSpec.builder("lib-2")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .build()
          )
          .addSrcSpec(
            ProjectSrcSpec.builder(Path.of("src/main/kotlin"))
              .addFile(
                FileSpec.builder("com.example.lib2", "Lib2Class.kt")
                  .addType(TypeSpec.classBuilder("Lib2Class").build())
                  .build()
              )
              .build()
          )
          .build()
      )
      .addSubproject(
        ProjectSpec.builder("lib-3")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .build()
          )
          .addSrcSpec(
            ProjectSrcSpec.builder(Path.of("src/main/kotlin"))
              .addFile(
                FileSpec.builder("com.example.lib3", "Lib3Class.kt")
                  .addType(TypeSpec.classBuilder("Lib3Class").build())
                  .build()
              )
              .build()
          )
          .build()
      )
      .build()
      .writeIn(testProjectDir.toPath())

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withDebug(true)
      .withProjectDir(testProjectDir)
      .withArguments("moduleCheckOvershot")
      .build()

    result.task(":moduleCheckOvershot")!!.outcome shouldBe TaskOutcome.SUCCESS

    File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  api(project(path = ":lib-3"))
        |}
        |""".trimMargin()
  }

  "unused inherited api dependencies should not be added" {

    projectSpecBuilder
      .addSubproject(
        ProjectSpec.builder("app")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .addProjectDependency("api", "lib-4")
              .build()
          )
          .addSrcSpec(
            ProjectSrcSpec.builder(Path.of("src/main/kotlin"))
              .addFile(
                FileSpec.builder("com.example.app", "MyApp.kt")
                  .addImport("com.example.lib1", "Lib1Class")
                  .addImport("com.example.lib2", "Lib2Class")
                  .build()
              )
              .build()
          )
          .build()
      )
      .addSubproject(
        ProjectSpec.builder("lib-1")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .build()
          )
          .addSrcSpec(
            ProjectSrcSpec.builder(Path.of("src/main/kotlin"))
              .addFile(
                FileSpec.builder("com.example.lib1", "Lib1Class.kt")
                  .addType(TypeSpec.classBuilder("Lib1Class").build())
                  .build()
              )
              .build()
          )
          .build()
      )
      .addSubproject(
        ProjectSpec.builder("lib-2")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .build()
          )
          .addSrcSpec(
            ProjectSrcSpec.builder(Path.of("src/main/kotlin"))
              .addFile(
                FileSpec.builder("com.example.lib2", "Lib2Class.kt")
                  .addType(TypeSpec.classBuilder("Lib2Class").build())
                  .build()
              )
              .build()
          )
          .build()
      )
      .addSubproject(
        ProjectSpec.builder("lib-3")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .build()
          )
          .addSrcSpec(
            ProjectSrcSpec.builder(Path.of("src/main/kotlin"))
              .addFile(
                FileSpec.builder("com.example.lib3", "Lib3Class.kt")
                  .addType(TypeSpec.classBuilder("Lib3Class").build())
                  .build()
              )
              .build()
          )
          .build()
      )
      .addSubproject(
        ProjectSpec.builder("lib-4")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .addProjectDependency("api", "lib-1")
              .addProjectDependency("api", "lib-2")
              .addProjectDependency("api", "lib-3")
              .build()
          )
          .build()
      )
      .build()
      .writeIn(testProjectDir.toPath())

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withDebug(true)
      .withProjectDir(testProjectDir)
      .withArguments("moduleCheckOvershot")
      .build()

    result.task(":moduleCheckOvershot")!!.outcome shouldBe TaskOutcome.SUCCESS

    File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  api(project(path = ":lib-1"))
        |  api(project(path = ":lib-2"))
        |  api(project(path = ":lib-4"))
        |}
        |""".trimMargin()
  }
})
