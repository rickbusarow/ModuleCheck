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
import com.squareup.kotlinpoet.TypeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import modulecheck.specs.*
import modulecheck.specs.ProjectSrcSpecBuilder.XmlFile
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

class UnusedDependenciesTest : BaseTest() {

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

  val jvmSub1 = jvmSubProject("lib-1", ClassName("com.example.lib1", "Lib1Class"))
  val jvmSub2 = jvmSubProject("lib-2", ClassName("com.example.lib2", "Lib2Class"))
  val jvmSub3 = jvmSubProject("lib-3", ClassName("com.example.lib3", "Lib3Class"))

  @Nested
  inner class `unused dependencies` {

    private val myApp = FileSpec.builder("com.example.app", "MyApp")
      .addImport("com.example.lib1", "Lib1Class")
      .build()

    val appProject = ProjectSpec("app") {
      addBuildSpec(
        ProjectBuildSpec {
          addPlugin("kotlin(\"jvm\")")
          addProjectDependency("api", jvmSub1)
          addProjectDependency("api", jvmSub2)
          addProjectDependency("implementation", jvmSub3)
        }
      )
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/main/kotlin")) {
          addFileSpec(myApp)
        }
      )
    }

    @Test
    fun `with autoCorrect should be removed`() {
      ProjectSpec("project") {
        applyEach(projects) { project ->
          addSubproject(project)
        }
        addSubproject(appProject)
        addSubprojects(jvmSub1, jvmSub2, jvmSub3)
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

      build("moduleCheckUnusedDependency").shouldSucceed()

      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  api(project(path = ":lib-1"))
        |  // api(project(path = ":lib-2"))  // ModuleCheck finding [unused]
        |  // implementation(project(path = ":lib-3"))  // ModuleCheck finding [unused]
        |}
        |""".trimMargin()
    }

    @Test
    fun `without autoCorrect should fail with report`() {
      ProjectSpec("project") {
        applyEach(projects) { project ->
          addSubproject(project)
        }
        addSubproject(appProject)
        addSubprojects(jvmSub1, jvmSub2, jvmSub3)
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
        "moduleCheckUnusedDependency"
      ) {
        it shouldContain "ModuleCheck found 2 issues"
        it shouldContain "> ModuleCheck found 2 issues which were not auto-corrected."
        it shouldContain "app/build.gradle.kts: (7, 3):  unused: :lib-2"
        it shouldContain "app/build.gradle.kts: (8, 3):  unused: :lib-3"
      }
    }
  }

  @Test
  fun `module with a custom view used in a layout subject module should not be unused`() {
    val activity_main_xml = XmlFile(
      "activity_main.xml",
      """<?xml version="1.0" encoding="utf-8"?>
        |<androidx.constraintlayout.widget.ConstraintLayout
        |  xmlns:android="http://schemas.android.com/apk/res/android"
        |  android:id="@+id/fragment_container"
        |  android:layout_width="match_parent"
        |  android:layout_height="match_parent"
        |  >
        |
        |  <com.example.lib1.Lib1View
        |    android:layout_width="match_parent"
        |    android:layout_height="match_parent"
        |    />
        |
        |</androidx.constraintlayout.widget.ConstraintLayout>
                """.trimMargin()
    )
    val appProject = ProjectSpec("app") {
      addBuildSpec(
        ProjectBuildSpec {
          addPlugin("""id("com.android.library")""")
          addPlugin("kotlin(\"android\")")
          android = true
          addProjectDependency("api", jvmSub1)
        }
      )
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/main/res/layout")) {
          addXmlFile(activity_main_xml)
        }
      )
    }

    ProjectSpec("project") {
      applyEach(projects) { project ->
        addSubproject(project)
      }
      addSubproject(appProject)
      addSubproject(
        ProjectSpec("lib-1") {
          addSrcSpec(
            ProjectSrcSpec(Path.of("src/main/java")) {
              addFileSpec(
                FileSpec.builder("com.example.lib1", "Lib1View")
                  .addType(TypeSpec.classBuilder("Lib1View").build())
                  .build()
              )
            }
          )
          addBuildSpec(
            ProjectBuildSpec {
              addPlugin("kotlin(\"jvm\")")
              addProjectDependency("api", jvmSub1)
            }
          )
        }
      )
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

    build("moduleCheckUnusedDependency").shouldSucceed()
  }
}
