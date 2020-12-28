package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.specs.ProjectBuildSpec
import com.rickbusarow.modulecheck.specs.ProjectSettingsSpec
import com.rickbusarow.modulecheck.specs.ProjectSpec
import com.rickbusarow.modulecheck.specs.ProjectSrcSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Path

class UnusedKaptTest : FreeSpec({

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

  "unused" {

    projectSpecBuilder
      .addSubproject(
        ProjectSpec.builder("app")
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .addPlugin("kotlin(\"kapt\")")
              .addExternalDependency("kapt", "com.google.dagger:dagger-compiler:2.30.1")
              .addExternalDependency("kaptTest", "com.squareup.moshi:moshi-kotlin-codegen:1.11.0")
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
      .withArguments("moduleCheckKapt")
      .build()

    println(result.output)

    result.task(":moduleCheckKapt")?.outcome shouldBe TaskOutcome.SUCCESS
  }

  "used" {

    projectSpecBuilder
      .addSubproject(
        ProjectSpec.builder("app")
          .addSrcSpec(
            ProjectSrcSpec.builder(Path.of("src/main/kotlin"))
              .addFile(
                FileSpec.builder("com.my.app", "App.kt")
                  .addType(
                    TypeSpec.classBuilder("MyClass")
                      .addFunction(
                        FunSpec.constructorBuilder()
                          .addAnnotation(ClassName("javax.inject", "Inject"))
                          .build()
                      )
                      .build()
                  )
                  .build()
              )
              .build()
          )
          .addBuildSpec(
            ProjectBuildSpec.builder()
              .addPlugin("kotlin(\"jvm\")")
              .addPlugin("kotlin(\"kapt\")")
              .addExternalDependency("kapt", "com.google.dagger:dagger-compiler:2.30.1")
              .addExternalDependency("kaptTest", "com.squareup.moshi:moshi-kotlin-codegen:1.11.0")
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
      .withArguments("moduleCheckKapt")
      .build()

    println(result.output)

    result.task(":moduleCheckKapt")?.outcome shouldBe TaskOutcome.SUCCESS
  }
})
