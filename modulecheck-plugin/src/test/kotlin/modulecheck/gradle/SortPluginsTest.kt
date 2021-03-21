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

import io.kotest.matchers.shouldBe
import modulecheck.specs.ProjectBuildSpec
import modulecheck.specs.ProjectSettingsSpec
import modulecheck.specs.ProjectSpec
import modulecheck.specs.ProjectSrcSpec
import modulecheck.specs.ProjectSrcSpecBuilder.KtsFile
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.nio.file.Path

class SortPluginsTest : BaseTest() {

  @TestFactory
  fun `sorting`() = test(
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
              addKtsFile(KtsFile("androidLibrary.gradle.kts", ""))
              addKtsFile(KtsFile("javaLibrary.gradle.kts", ""))
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
  ) {

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
