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
import com.squareup.kotlinpoet.ClassName
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

  val projectSettings = ProjectSettingsSpec.builder()
    .applyEach(projects) { project ->
      addInclude(project.gradlePath)
    }
    .addInclude("app")

  val projectBuild = ProjectBuildSpec.builder()
    // .addImport("import com.rickbusarow.modulecheck.moduleCheck")
    .addPlugin("id(\"com.rickbusarow.module-check\")")
    .buildScript()

  val projectSpecBuilder = ProjectSpec.builder("project")
    .applyEach(projects) { project ->
      addSubproject(project)
    }

  val jvmSub1 = jvmSubProject("lib-1", ClassName("com.example.lib1", "Lib1Class"))
  val jvmSub2 = jvmSubProject("lib-2", ClassName("com.example.lib2", "Lib2Class"))
  val jvmSub3 = jvmSubProject("lib-3", ClassName("com.example.lib3", "Lib3Class"))

  "overshot dependencies" - {

    val b = jvmSub3.toBuilder()

    b.projectBuildSpec?.let { bs ->
      b.addBuildSpec(
        bs.toBuilder()
          .addProjectDependency("api", jvmSub1)
          .addProjectDependency("api", jvmSub2)
          .build()
      )
    }

    projectSpecBuilder
      .addSubproject(
        ProjectSpec.builder("app")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .addProjectDependency("api", jvmSub1)
              .addProjectDependency("api", jvmSub2)
              .addProjectDependency("api", jvmSub3)
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
      .addSubprojects(jvmSub1, jvmSub2)
      .addSubproject(
        ProjectSpec.builder("lib-3")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .addProjectDependency("api", jvmSub1)
              .addProjectDependency("api", jvmSub2)
              .build()
          )
          .build()
      )

    "with autoCorrect" - {

      projectSpecBuilder
        .addSettingsSpec(projectSettings.build())
        .addBuildSpec(
          projectBuild
            .addBlock(
              """moduleCheck {
            |  // autoCorrect.set(true)
            |}
          """.trimMargin()
            ).build()
        )

      "should be added" {

        projectSpecBuilder
          .build()
          // .writeIn(File("FFFF").toPath())
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
    }
  }

  "added dependencies should match the configuration of the dependency which provided them" {

    projectSpecBuilder
      .addSubproject(
        ProjectSpec.builder("app")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .addProjectDependency("implementation", jvmSub1)
              .addProjectDependency("implementation", jvmSub2)
              .addProjectDependency("implementation", jvmSub3)
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
      .addSubprojects(jvmSub1, jvmSub2)
      .addSubproject(
        ProjectSpec.builder("lib-3")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .addProjectDependency("api", jvmSub1)
              .addProjectDependency("api", jvmSub2)
              .build()
          )
          .build()
      )
    projectSpecBuilder
      .addSettingsSpec(projectSettings.build())
      .addBuildSpec(
        projectBuild
          .addBlock(
            """moduleCheck {
            |  // autoCorrect.set(true)
            |}
          """.trimMargin()
          ).build()
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
              .addProjectDependency("api", jvmSub1)
              .addProjectDependency("api", jvmSub2)
              .addProjectDependency("api", jvmSub3)
              .build()
          )
          .addSrcSpec(
            ProjectSrcSpec.builder(Path.of("src/main/kotlin"))
              .addFile(
                FileSpec.builder("com.example.app", "MyApp.kt")
                  .addImport("com.example.lib3", "Lib3Class")
                  .build()
              )
              .build()
          )
          .build()
      )
      .addSubprojects(jvmSub1, jvmSub2, jvmSub3)
      .addSettingsSpec(projectSettings.build())
      .addBuildSpec(
        projectBuild
          .addBlock(
            """moduleCheck {
            |  // autoCorrect.set(true)
            |}
          """.trimMargin()
          ).build()
      )

    projectSpecBuilder
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

  "unused inherited api dependencies should not be added" {

    projectSpecBuilder
      .addSubproject(
        ProjectSpec.builder("app")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .addProjectDependency("api", "lib-1")
              .addProjectDependency("api", "lib-2")
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
      .addSubproject(jvmSub1)
      .addSubproject(jvmSub2)
      .addSubproject(jvmSub3)
      .addSubproject(jvmSubProject("lib-4", apiDependencies = listOf(jvmSub1, jvmSub2, jvmSub3)))

      .addSettingsSpec(projectSettings.build())
      .addBuildSpec(
        projectBuild.addBlock(
          """moduleCheck {
            |  // autoCorrect.set(true)
            |}
          """.trimMargin()
        ).build()
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

@Suppress("LongParameterList")
fun jvmSubProject(
  path: String,
  vararg fqClassName: ClassName,
  apiDependencies: List<ProjectSpec> = emptyList(),
  implementationDependencies: List<ProjectSpec> = emptyList(),
  androidTestDependencies: List<ProjectSpec> = emptyList(),
  testDependencies: List<ProjectSpec> = emptyList()
): ProjectSpec = ProjectSpec.builder(path)
  .addBuildSpec(
    ProjectBuildSpec.builder()
      .addPlugin("kotlin(\"jvm\")")
      .applyEach(apiDependencies) { addProjectDependency("api", it) }
      .applyEach(implementationDependencies) { addProjectDependency("implementation", it) }
      .applyEach(androidTestDependencies) { addProjectDependency("androidTestImplementation", it) }
      .applyEach(testDependencies) { addProjectDependency("testImplementation", it) }
      .build()
  )
  .applyEach(fqClassName.toList()) { fq ->
    addSrcSpec(
      ProjectSrcSpec.builder(Path.of("src/main/kotlin"))
        .addFile(
          FileSpec.builder(fq.packageName, fq.simpleName + ".kt")
            .addType(TypeSpec.classBuilder(fq.simpleName).build())
            .build()
        )
        .build()
    )
  }
  .build()
