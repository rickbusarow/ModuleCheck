package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.internal.files
import com.rickbusarow.modulecheck.testing.*
import com.squareup.kotlinpoet.FileSpec
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Path

class RubberDucky : FreeSpec({

  val testProjectDir = tempDir()

  fun File.relativePath() = path.removePrefix(testProjectDir.path)

  "test" - {

    val ps = ProjectSpec.Builder("app")
      .addSettingsSpec(
        ProjectSettingsSpec.Builder()
          .addInclude("lib-a")
          .addInclude("lib-1")
          .addInclude("lib-2")
          .addInclude("lib-3")
          .addInclude("lib-4")
          .build()
      )
      .addBuildSpec(
        ProjectBuildSpec.Builder()
          .addPlugin("id(\"com.rickbusarow.module-check\")")
          .buildScript()
          .build()
      )
      .addSubproject(
//        ProjectSpec.Builder("lib-a")
////          .addSrcSpec(
////            ProjectSrcSpec.Builder(Path.of("src/main/java"))
////              .addFile(FileSpec.builder("com.foo", "Bar").build())
////              .build()
////          )
//          .addBuildSpec(
//            ProjectBuildSpec.Builder()
//              .addPlugin("id(\"com.android.application\")")
//              .addPlugin("kotlin(\"android\")")
//              .android()
////              .addDependency("implementation", "lib-1")
////              .addDependency("implementation", "lib-2")
////              .addDependency("implementation", "lib-3")
////              .addDependency("implementation", "lib-4")
//              .build()
//          )
//          .build()
          ProjectSpec.Builder("lib-a")
          .addSrcSpec(
            ProjectSrcSpec.Builder(Path.of("src/main/java"))
              .addFile(FileSpec.builder("com.foo", "Bar").build())
              .build()
          )
          .addBuildSpec(
            ProjectBuildSpec.Builder()
              .addPlugin("kotlin(\"jvm\")")
              .addDependency("implementation", "lib-1")
              .addDependency("implementation", "lib-2")
              .addDependency("implementation", "lib-3")
              .addDependency("implementation", "lib-4")
              .build()
          )
          .build()
      )
      .addSubproject(
        ProjectSpec.Builder("lib-1")
          .addSrcSpec(
            ProjectSrcSpec.Builder(Path.of("src/main/java"))
              .addFile(FileSpec.builder("com.foo", "Bar").build())
              .build()
          )
          .addBuildSpec(
            ProjectBuildSpec.Builder()
              .addPlugin("kotlin(\"jvm\")")
              .addDependency("implementation", "lib-2")
              .addDependency("implementation", "lib-3")
              .addDependency("implementation", "lib-4")
              .build()
          )
          .build()
      )
      .addSubproject(
        ProjectSpec.Builder("lib-2")
          .addSrcSpec(
            ProjectSrcSpec.Builder(Path.of("src/main/java"))
              .addFile(FileSpec.builder("com.foo", "Bar").build())
              .build()
          )
          .addBuildSpec(
            ProjectBuildSpec.Builder()
              .addPlugin("kotlin(\"jvm\")")
              .addDependency("implementation", "lib-3")
              .addDependency("implementation", "lib-4")
              .build()
          )
          .build()
      )
      .addSubproject(
        ProjectSpec.Builder("lib-3")
          .addSrcSpec(
            ProjectSrcSpec.Builder(Path.of("src/main/java"))
              .addFile(FileSpec.builder("com.foo", "Bar").build())
              .build()
          )
          .addBuildSpec(
            ProjectBuildSpec.Builder()
              .addPlugin("kotlin(\"jvm\")")
              .addDependency("implementation", "lib-4")
              .build()
          )
          .build()
      )
      .addSubproject(
        ProjectSpec.Builder("lib-4")
          .addSrcSpec(
            ProjectSrcSpec.Builder(Path.of("src/main/java"))
              .addFile(FileSpec.builder("com.foo", "Bar").build())
              .build()
          )
          .addBuildSpec(
            ProjectBuildSpec.Builder()
              .addPlugin("kotlin(\"jvm\")")
              .build()
          )
          .build()
      )
      .build()

    ps.writeIn(testProjectDir.toPath())

    "ducky task" {

      testProjectDir
        .walkTopDown()
        .files()
        .forEach {
          println(it.relativePath())
          println(it.readText())
          println()
        }

      val result = GradleRunner.create()
        .withPluginClasspath()
        .withDebug(true)
        .withProjectDir(testProjectDir)
        .withArguments("moduleCheck")
        .build()

      println(result.output)

      result.task(":moduleCheck")?.outcome shouldBe TaskOutcome.SUCCESS
    }

  }
})
