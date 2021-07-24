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

import com.squareup.kotlinpoet.*
import io.kotest.matchers.string.shouldContain
import modulecheck.specs.*
import modulecheck.specs.ProjectSrcSpecBuilder.RawFile
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

  private val lib1ClassName = ClassName("com.example.lib1", "Lib1Class")

  val jvmSub1 = jvmSubProject("lib-1", lib1ClassName)
  val jvmSub2 = jvmSubProject("lib-2", ClassName("com.example.lib2", "Lib2Class"))
  val jvmSub3 = jvmSubProject("lib-3", ClassName("com.example.lib3", "Lib3Class"))

  @Nested
  inner class `unused dependencies` {

    private val myApp = FileSpec.builder("com.example.app", "MyApp")
      .addImport("com.example.lib1", "Lib1Class")
      .build()

    @Test
    fun `with autoCorrect should be removed`() {
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
    fun `with autoCorrect using string extension functions for configuration should be removed`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addProjectDependency("\"api\"", jvmSub1)
            addProjectDependency("\"api\"", jvmSub2)
            addProjectDependency("\"implementation\"", jvmSub3)
            addPlugin("kotlin(\"jvm\")")
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/kotlin")) {
            addFileSpec(myApp)
          }
        )
      }
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
        |  "api"(project(path = ":lib-1"))
        |  // "api"(project(path = ":lib-2"))  // ModuleCheck finding [unused]
        |  // "implementation"(project(path = ":lib-3"))  // ModuleCheck finding [unused]
        |}
        |""".trimMargin()
    }

    @Test
    fun `without autoCorrect should fail with report`() {
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

      shouldFailWithMessage("moduleCheckUnusedDependency") {
        it shouldContain "> ModuleCheck found 2 issues which were not auto-corrected."
        it shouldContain "app/build.gradle.kts: (7, 3):  unused: :lib-2"
        it shouldContain "app/build.gradle.kts: (8, 3):  unused: :lib-3"
      }
    }

    @Test
    fun `without autoCorrect using string extension functions for configuration should fail with report`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("kotlin(\"jvm\")")
            addProjectDependency("\"api\"", jvmSub1)
            addProjectDependency("\"api\"", jvmSub2)
            addProjectDependency("\"implementation\"", jvmSub3)
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/kotlin")) {
            addFileSpec(myApp)
          }
        )
      }
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

      shouldFailWithMessage("moduleCheckUnusedDependency") {
        it shouldContain "> ModuleCheck found 2 issues which were not auto-corrected."
        it shouldContain "app/build.gradle.kts: (7, 3):  unused: :lib-2"
        it shouldContain "app/build.gradle.kts: (8, 3):  unused: :lib-3"
      }
    }
  }

  @Test
  fun `testImplementation used in test should not be unused`() {
    val myTest = FileSpec.builder("com.example.app", "MyTest")
      .addImport("com.example.lib1", "Lib1Class")
      .build()

    val appProject = ProjectSpec("app") {
      addBuildSpec(
        ProjectBuildSpec {
          addPlugin("kotlin(\"jvm\")")
          addProjectDependency("testImplementation", jvmSub1)
        }
      )
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/test/kotlin")) {
          addFileSpec(myTest)
        }
      )
    }
    ProjectSpec("project") {
      applyEach(projects) { project ->
        addSubproject(project)
      }
      addSubproject(appProject)
      addSubprojects(jvmSub1)
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

  @Test
  fun `androidTestImplementation used in android test should not be unused`() {
    val appProject = ProjectSpec("app") {
      addBuildSpec(
        ProjectBuildSpec {
          android = true
          addPlugin("""id("com.android.library")""")
          addPlugin("kotlin(\"android\")")
          addProjectDependency("androidTestImplementation", jvmSub1)
        }
      )
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/androidTest/java")) {
          addFileSpec(
            FileSpec.builder("com.example.app", "MyTest")
              .addType(
                TypeSpec.classBuilder("MyTest")
                  .primaryConstructor(
                    FunSpec.constructorBuilder()
                      .addParameter("lib1Class", lib1ClassName)
                      .build()
                  )
                  .build()
              )
              .build()
          )
        }
      )
    }
    ProjectSpec("project") {
      applyEach(projects) { project ->
        addSubproject(project)
      }
      addSubproject(appProject)
      addSubprojects(jvmSub1)
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

  @Test
  fun `module with a custom view used in a layout subject module should not be unused`() {
    val activity_main_xml = RawFile(
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
          addRawFile(activity_main_xml)
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
                  .addType(
                    TypeSpec.classBuilder("Lib1View")
                      .addAnnotation(
                        AnnotationSpec
                          .builder(ClassName.bestGuess("com.squareup.anvil.annotations.ContributesMultibinding"))
                          .addMember("%T", ClassName.bestGuess("com.example.lib1.AppScope"))
                          .build()
                      )
                      .build()
                  )
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

  @Test
  fun `module with a declaration used via a wildcard import should not be unused`() {

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
        ProjectSrcSpec(Path.of("src/main/java")) {
          addRawFile(
            "MyFile.kt", """
            package com.example.app

            import com.example.lib1.*

            val theView = Lib1View()
          """.trimIndent()
          )
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
                  .addType(
                    TypeSpec.classBuilder("Lib1View")
                      .addAnnotation(
                        AnnotationSpec
                          .builder(ClassName.bestGuess("com.squareup.anvil.annotations.ContributesMultibinding"))
                          .addMember("%T", ClassName.bestGuess("com.example.lib1.AppScope"))
                          .build()
                      )
                      .build()
                  )
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

  @Test
  fun `module with a static member declaration used via a wildcard import should not be unused`() {

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
        ProjectSrcSpec(Path.of("src/main/java")) {
          addRawFile(
            "MyFile.kt", """
            package com.example.app

            import com.example.lib1.*

            val theView = Lib1View.build()
          """.trimIndent()
          )
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
                  .addType(
                    TypeSpec.classBuilder("Lib1View")
                      .addType(
                        TypeSpec.companionObjectBuilder()
                          .addFunction(
                            FunSpec.builder("build")
                              .build()
                          )
                          .build()
                      )
                      .build()
                  )
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

  @Test
  fun `module with a string resource used in subject module should not be unused`() {
    val appFile = FileSpec.builder("com.example.app", "MyApp")
      .addType(
        TypeSpec.classBuilder("MyApp")
          .addProperty(
            PropertySpec.builder("appNameRes", Int::class.asTypeName())
              .getter(
                FunSpec.getterBuilder()
                  .addCode(
                    """return %T.string.app_name""",
                    ClassName.bestGuess("com.example.app.R")
                  )
                  .build()
              )
              .build()
          )
          .build()
      )
      .build()

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
        ProjectSrcSpec(Path.of("src/main/java")) {
          addFileSpec(appFile)
        }
      )
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/main")) {
          addRawFile(
            RawFile(
              "AndroidManifest.xml",
              """<manifest package="com.example.app" />
                """.trimMargin()
            )
          )
        }
      )
    }

    val androidSub1 = ProjectSpec("lib-1") {
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/main/res/values")) {
          addRawFile(
            RawFile(
              "strings.xml",
              """<resources>
                |  <string name="app_name" translatable="false">MyApp</string>
                |</resources>
                """.trimMargin()
            )
          )
        }
      )
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/main")) {
          addRawFile(
            RawFile(
              "AndroidManifest.xml",
              """<manifest package="com.example.lib1" />
                """.trimMargin()
            )
          )
        }
      )
      addBuildSpec(
        ProjectBuildSpec {
          addPlugin("""id("com.android.library")""")
          addPlugin("kotlin(\"android\")")
          android = true
        }
      )
    }

    ProjectSpec("project") {
      addSubproject(appProject)
      addSubproject(androidSub1)
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
