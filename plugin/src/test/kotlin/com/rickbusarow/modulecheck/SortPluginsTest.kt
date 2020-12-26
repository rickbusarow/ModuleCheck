package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.testing.ProjectBuildSpec
import com.rickbusarow.modulecheck.testing.ProjectSettingsSpec
import com.rickbusarow.modulecheck.testing.ProjectSpec
import com.rickbusarow.modulecheck.testing.ProjectSrcSpec
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

  val projectSpecBuilder = ProjectSpec.Builder("project")
    .addSettingsSpec(
      ProjectSettingsSpec.Builder()
        .addInclude("app")
        .build()
    )
    .addBuildSpec(
      ProjectBuildSpec.Builder()
        .addPlugin("id(\"com.rickbusarow.module-check\")")
        .buildScript()
        .build()
    )
    .addSubproject(
      ProjectSpec.Builder("buildSrc")
        .addBuildSpec(
          ProjectBuildSpec.Builder()
            .addPlugin("`kotlin-dsl`")
            .addRepository("mavenCentral()")
            .addRepository("google()")
            .addRepository("jcenter()")
            .build()
        )
        .addSrcSpec(
          ProjectSrcSpec.Builder(Path.of("src/main/kotlin"))
            .addFile(FileSpec.builder("", "androidLibrary.gradle.kts").build())
            .addFile(FileSpec.builder("", "javaLibrary.gradle.kts").build())
            .build()
        )
        .build()
    )

  "sorting" {

    projectSpecBuilder
      .addSubproject(
        ProjectSpec.Builder("app")
          .addBuildSpec(
            ProjectBuildSpec.Builder()
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
