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
import com.rickbusarow.modulecheck.testing.ProjectSrcSpec
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
// //          .addSrcSpec(
// //            ProjectSrcSpec.Builder(Path.of("src/main/java"))
// //              .addFile(FileSpec.builder("com.foo", "Bar").build())
// //              .build()
// //          )
//          .addBuildSpec(
//            ProjectBuildSpec.Builder()
//              .addPlugin("id(\"com.android.application\")")
//              .addPlugin("kotlin(\"android\")")
//              .android()
// //              .addDependency("implementation", "lib-1")
// //              .addDependency("implementation", "lib-2")
// //              .addDependency("implementation", "lib-3")
// //              .addDependency("implementation", "lib-4")
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
              .addProjectDependency("implementation", "lib-1")
              .addProjectDependency("implementation", "lib-2")
              .addProjectDependency("implementation", "lib-3")
              .addProjectDependency("implementation", "lib-4")
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
              .addProjectDependency("implementation", "lib-2")
              .addProjectDependency("implementation", "lib-3")
              .addProjectDependency("implementation", "lib-4")
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
              .addProjectDependency("implementation", "lib-3")
              .addProjectDependency("implementation", "lib-4")
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
              .addProjectDependency("implementation", "lib-4")
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

//      testProjectDir
//        .walkTopDown()
//        .files()
//        .forEach {
//          println(it.relativePath())
//          println(it.readText())
//          println()
//        }

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
