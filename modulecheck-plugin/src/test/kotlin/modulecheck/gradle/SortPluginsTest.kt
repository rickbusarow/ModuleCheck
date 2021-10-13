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

import modulecheck.specs.ProjectBuildSpec
import modulecheck.specs.ProjectSettingsSpec
import modulecheck.specs.ProjectSpec
import modulecheck.specs.ProjectSrcSpec
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

class SortPluginsTest : BasePluginTest() {

  @Test
  fun `sorting`() {
    ProjectSpec("project") {
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
              addRawFile("androidLibrary.gradle.kts", "")
              addRawFile("javaLibrary.gradle.kts", "")
            }
          )
        }
      )
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

    build("moduleCheckSortPlugins").shouldSucceed()

    File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |  javaLibrary
        |  id("io.gitlab.arturbosch.detekt") version "1.15.0"
        |}
        |
        |""".trimMargin()
  }

  @Test
  fun `sorting should be idempotent`() {
    ProjectSpec("project") {
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
              addRawFile("androidLibrary.gradle.kts", "")
              addRawFile("javaLibrary.gradle.kts", "")
            }
          )
        }
      )
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

    build("moduleCheckSortPlugins").shouldSucceed()
    build("moduleCheckSortPlugins").shouldSucceed()

    File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |  javaLibrary
        |  id("io.gitlab.arturbosch.detekt") version "1.15.0"
        |}
        |
        |""".trimMargin()
  }
}
