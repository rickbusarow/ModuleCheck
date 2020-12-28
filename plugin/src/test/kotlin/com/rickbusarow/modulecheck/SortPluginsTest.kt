package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.specs.ProjectBuildSpec
import com.rickbusarow.modulecheck.specs.ProjectSettingsSpec
import com.rickbusarow.modulecheck.specs.ProjectSpec
import com.rickbusarow.modulecheck.specs.ProjectSrcSpec
import com.squareup.kotlinpoet.FileSpec
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Path

class SortPluginsTest : FreeSpec({

  val testProjectDir = tempDir()

  fun File.relativePath() = path.removePrefix(testProjectDir.path)

  val projectSpecBuilder = ProjectSpec.builder("project")
    .addSettingsSpec(
      ProjectSettingsSpec.builder()
        .addInclude("app")
        .build()
    )
    .addBuildSpec(
      ProjectBuildSpec.builder()
        .addPlugin("id(\"com.rickbusarow.module-check\")")
        .buildScript()
        .build()
    )
    .addSubproject(
      ProjectSpec.builder("buildSrc")
        .addBuildSpec(
          ProjectBuildSpec.builder()
            .addPlugin("`kotlin-dsl`")
            .addRepository("mavenCentral()")
            .addRepository("google()")
            .addRepository("jcenter()")
            .build()
        )
        .addSrcSpec(
          ProjectSrcSpec.builder(Path.of("src/main/kotlin"))
            .addFile(FileSpec.builder("", "androidLibrary.gradle.kts").build())
            .addFile(FileSpec.builder("", "javaLibrary.gradle.kts").build())
            .build()
        )
        .build()
    )

  "sorting" {

    projectSpecBuilder
      .addSubproject(
        ProjectSpec.builder("app")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("javaLibrary")
              .addPlugin("kotlin(\"jvm\")")
              .addPlugin("id(\"io.gitlab.arturbosch.detekt\") version \"1.15.0\"")
              .build()
          )
          .build()
      )

    projectSpecBuilder
      .build()
      .writeIn(testProjectDir.toPath())

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withDebug(true)
      .withProjectDir(testProjectDir)
      .withArguments("moduleCheckSortPlugins")
      .build()

    result.task(":moduleCheckSortPlugins")?.outcome shouldBe TaskOutcome.SUCCESS

    File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |  javaLibrary
        |  id("io.gitlab.arturbosch.detekt") version "1.15.0"
        |}
        |
        |""".trimMargin()
  }
})
