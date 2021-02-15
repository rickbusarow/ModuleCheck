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

import com.rickbusarow.modulecheck.specs.ProjectBuildSpec
import com.rickbusarow.modulecheck.specs.ProjectSettingsSpec
import com.rickbusarow.modulecheck.specs.ProjectSpec
import com.rickbusarow.modulecheck.specs.ProjectSrcSpec
import com.squareup.kotlinpoet.FileSpec
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

class SortPluginsTest : BaseTest() {

  fun File.relativePath() = path.removePrefix(testProjectDir.path)

  val projectSpec = ProjectSpec("project") {
    addSettingsSpec(
      ProjectSettingsSpec {
        addInclude("app")
      }
    )
    addBuildSpec(
      ProjectBuildSpec {
        addPlugin("id(\"com.rickbusarow.module-check\")")
        buildScript()
      }
    )
    addSubproject(
      ProjectSpec("buildSrc") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("`kotlin-dsl`")
            addRepository("mavenCentral()")
            addRepository("google()")
            addRepository("jcenter()")
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/kotlin")) {
            addFile(FileSpec.builder("", "androidLibrary.gradle.kts").build())
            addFile(FileSpec.builder("", "javaLibrary.gradle.kts").build())
          }
        )
      }
    )
  }

  @Test
  fun `sorting`() {
    projectSpec
      .edit {
        addSubproject(
          ProjectSpec("app") {
            addBuildSpec(
              ProjectBuildSpec {
                addPlugin("javaLibrary")
                addPlugin("kotlin(\"jvm\")")
                addPlugin("id(\"io.gitlab.arturbosch.detekt\") version \"1.15.0\"")
              }
            )
          }
        )
      }
      .writeIn(testProjectDir.toPath())

    val result = gradleRunner
      .withArguments("moduleCheckSortPlugins")
      .build()

    result.task(":moduleCheckSortPlugins")?.outcome shouldBe SUCCESS

    File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |  javaLibrary
        |  id("io.gitlab.arturbosch.detekt") version "1.15.0"
        |}
        |
        |""".trimMargin()
  }
}
