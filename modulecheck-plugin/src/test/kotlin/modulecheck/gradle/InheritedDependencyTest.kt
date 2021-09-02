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

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import io.kotest.matchers.string.shouldContain
import modulecheck.specs.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

class InheritedDependencyTest : BaseTest() {

  val projects = List(10) {
    ProjectSpec.builder("lib-$it")
      .build()
  }

  val projectSettings = ProjectSettingsSpecBuilder {
    projects.forEach { project ->
      addInclude(project.gradlePath)
    }
    addInclude("app")
  }

  val projectBuild = ProjectBuildSpecBuilder()
    .addPlugin("id(\"com.rickbusarow.module-check\")")
    .buildScript()

  val lib1ClassName = ClassName("com.example.lib1", "Lib1Class")
  val lib2ClassName = ClassName("com.example.lib2", "Lib2Class")
  val lib3ClassName = ClassName("com.example.lib3", "Lib3Class")
  val lib4ClassName = ClassName("com.example.lib4", "Lib4Class")

  val jvmSub1 = jvmSubProject("lib-1", lib1ClassName)
  val jvmSub2 = jvmSubProject("lib-2", lib2ClassName)
  val jvmSub3 = jvmSubProject("lib-3", lib3ClassName)
  val jvmSub4 = jvmSubProject("lib-4", lib4ClassName)

  @Nested
  inner class `deeply inherited` {

    @Nested
    inner class `with auto-correct` {
      @Test
      fun `androidTestImplementation should add inherited projects and succeed`() {
        val appProject = ProjectSpec("app") {
          addBuildSpec(
            ProjectBuildSpec {
              addPlugin("""id("com.android.library")""")
              android = true
              addProjectDependency("androidTestImplementation", jvmSub4)
              addProjectDependency("api", jvmSub4)
            }
          )
          addSrcSpec(
            ProjectSrcSpec(Path.of("src/main/java")) {
              addFileSpec(
                FileSpec.builder("com.example.app", "MyApp")
                  .addProperty(PropertySpec.builder("lib1Class", lib1ClassName).build())
                  .addProperty(PropertySpec.builder("lib2Class", lib2ClassName).build())
                  .addProperty(PropertySpec.builder("lib3Class", lib3ClassName).build())
                  .addProperty(PropertySpec.builder("lib4Class", lib4ClassName).build())
                  .build()
              )
            }
          )
        }

        jvmSub4.edit {
          projectBuildSpec!!.edit {
            addProjectDependency("api", jvmSub3)
          }
        }
        jvmSub3.edit {
          projectBuildSpec!!.edit {
            addProjectDependency("api", jvmSub2)
          }
        }
        jvmSub2.edit {
          projectBuildSpec!!.edit {
            addProjectDependency("api", jvmSub1)
          }
        }
        ProjectSpec("project") {
          applyEach(projects) { project ->
            addSubproject(project)
          }
          addSubproject(appProject)
          addSubprojects(jvmSub1, jvmSub2, jvmSub3, jvmSub4)
          addSettingsSpec(projectSettings.build())
          addBuildSpec(
            projectBuild
              .addBlock(
                """moduleCheck {
            |  autoCorrect = true
            |}
          """.trimMargin()
              ).build()
          )
        }
          .writeIn(testProjectDir.toPath())

        build(
          "moduleCheckInheritedDependency",
          "moduleCheckSortDependencies"
        ).shouldSucceed()

        File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  id("com.android.library")
        |}
        |
        |android {
        |  compileSdkVersion(30)
        |
        |  defaultConfig {
        |    minSdkVersion(23)
        |    targetSdkVersion(30)
        |  }
        |
        |  buildTypes {
        |    getByName("release") {
        |      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        |    }
        |  }
        |}
        |
        |dependencies {
        |  androidTestImplementation(project(path = ":lib-4"))
        |
        |  api(project(path = ":lib-1"))
        |  api(project(path = ":lib-2"))
        |  api(project(path = ":lib-3"))
        |  api(project(path = ":lib-4"))
        |}
        |""".trimMargin()
      }

      @Test
      fun `should add inherited projects and succeed`() {
        val appProject = ProjectSpec("app") {
          addBuildSpec(
            ProjectBuildSpec {
              addPlugin("kotlin(\"jvm\")")
              addProjectDependency("api", jvmSub4)
            }
          )
          addSrcSpec(
            ProjectSrcSpec(Path.of("src/main/kotlin")) {
              addFileSpec(
                FileSpec.builder("com.example.app", "MyApp")
                  .addProperty(PropertySpec.builder("lib1Class", lib1ClassName).build())
                  .addProperty(PropertySpec.builder("lib2Class", lib2ClassName).build())
                  .addProperty(PropertySpec.builder("lib3Class", lib3ClassName).build())
                  .addProperty(PropertySpec.builder("lib4Class", lib4ClassName).build())
                  .build()
              )
            }
          )
        }

        jvmSub4.edit {
          projectBuildSpec!!.edit {
            addProjectDependency("api", jvmSub3)
          }
        }
        jvmSub3.edit {
          projectBuildSpec!!.edit {
            addProjectDependency("api", jvmSub2)
          }
        }
        jvmSub2.edit {
          projectBuildSpec!!.edit {
            addProjectDependency("api", jvmSub1)
          }
        }
        ProjectSpec("project") {
          applyEach(projects) { project ->
            addSubproject(project)
          }
          addSubproject(appProject)
          addSubprojects(jvmSub1, jvmSub2, jvmSub3, jvmSub4)
          addSettingsSpec(projectSettings.build())
          addBuildSpec(
            projectBuild
              .addBlock(
                """moduleCheck {
            |  autoCorrect = true
            |}
          """.trimMargin()
              ).build()
          )
        }
          .writeIn(testProjectDir.toPath())

        build(
          "moduleCheckInheritedDependency",
          "moduleCheckSortDependencies"
        ).shouldSucceed()

        File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  api(project(path = ":lib-1"))
        |  api(project(path = ":lib-2"))
        |  api(project(path = ":lib-3"))
        |  api(project(path = ":lib-4"))
        |}
        |""".trimMargin()
      }
    }

    @Nested
    inner class `without auto-correct` {

      @Test
      fun `should fail and name all three libraries as inherited`() {
        val appProject = ProjectSpec("app") {
          addBuildSpec(
            ProjectBuildSpec {
              addPlugin("kotlin(\"jvm\")")
              addProjectDependency("api", jvmSub4)
            }
          )
          addSrcSpec(
            ProjectSrcSpec(Path.of("src/main/kotlin")) {
              addFileSpec(
                FileSpec.builder("com.example.app", "MyApp")
                  .addProperty(PropertySpec.builder("lib1Class", lib1ClassName).build())
                  .addProperty(PropertySpec.builder("lib2Class", lib2ClassName).build())
                  .addProperty(PropertySpec.builder("lib3Class", lib3ClassName).build())
                  .addProperty(PropertySpec.builder("lib4Class", lib4ClassName).build())
                  .build()
              )
            }
          )
        }

        jvmSub4.edit {
          projectBuildSpec!!.edit {
            addProjectDependency("api", jvmSub3)
          }
        }
        jvmSub3.edit {
          projectBuildSpec!!.edit {
            addProjectDependency("api", jvmSub2)
          }
        }
        jvmSub2.edit {
          projectBuildSpec!!.edit {
            addProjectDependency("api", jvmSub1)
          }
        }
        ProjectSpec("project") {
          applyEach(projects) { project ->
            addSubproject(project)
          }
          addSubproject(appProject)
          addSubprojects(jvmSub1, jvmSub2, jvmSub3, jvmSub4)
          addSettingsSpec(projectSettings.build())
          addBuildSpec(
            projectBuild
              .addBlock(
                """moduleCheck {
            |  autoCorrect = false
            |}
          """.trimMargin()
              ).build()
          )
        }
          .writeIn(testProjectDir.toPath())

        shouldFailWithMessage(
          "moduleCheckInheritedDependency",
          "moduleCheckSortDependencies"
        ) {
          it shouldContain "app/build.gradle.kts: (6, 3):  inheritedDependency: :lib-1 from: :lib-4"
          it shouldContain "app/build.gradle.kts: (6, 3):  inheritedDependency: :lib-2 from: :lib-4"
          it shouldContain "app/build.gradle.kts: (6, 3):  inheritedDependency: :lib-3 from: :lib-4"
        }
      }
    }
  }
}
