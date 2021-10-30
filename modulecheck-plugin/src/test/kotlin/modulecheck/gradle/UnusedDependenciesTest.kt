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
import modulecheck.specs.*
import modulecheck.specs.ProjectSrcSpecBuilder.RawFile
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

class UnusedDependenciesTest : BasePluginTest() {

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
    fun `with autoCorrect should be commented out`() {
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

      shouldSucceed("moduleCheckUnusedDependency") withTrimmedMessage """:app
           dependency    name                source    build file
        ✔  :lib-2        unusedDependency              /app/build.gradle.kts: (7, 3):
        ✔  :lib-3        unusedDependency              /app/build.gradle.kts: (8, 3):

ModuleCheck found 2 issues"""

      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  api(project(path = ":lib-1"))
        |  // api(project(path = ":lib-2"))  // ModuleCheck finding [unusedDependency]
        |  // implementation(project(path = ":lib-3"))  // ModuleCheck finding [unusedDependency]
        |}
        |""".trimMargin()
    }

    @Test
    fun `suppressed dependency which is unused should not be reported`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("kotlin(\"jvm\")")
            addProjectDependency("api", jvmSub1)
            addProjectDependency("api", jvmSub2, "@Suppress(\"unusedDependency\")")
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

      shouldSucceed("moduleCheckUnusedDependency") withTrimmedMessage """:app
           dependency    name                source    build file
        ✔  :lib-3        unusedDependency              /app/build.gradle.kts: (9, 3):

ModuleCheck found 1 issues"""

      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  api(project(path = ":lib-1"))
        |  @Suppress("unusedDependency")
        |  api(project(path = ":lib-2"))
        |  // implementation(project(path = ":lib-3"))  // ModuleCheck finding [unusedDependency]
        |}
        |""".trimMargin()
    }

    @Test
    fun `suppressed unused at the dependency block should not report any unused`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("kotlin(\"jvm\")")
            addBlock(
              """
              |@Suppress("unusedDependency")
              |dependencies {
              |  api(project(path = ":lib-1"))
              |  api(project(path = ":lib-2"))
              |  implementation(project(path = ":lib-3"))
              |}
              |""".trimMargin()
            )
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
        addBuildSpec(projectBuild.build())
      }
        .writeIn(testProjectDir.toPath())

      shouldSucceed("moduleCheckUnusedDependency")

      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |@Suppress("unusedDependency")
        |dependencies {
        |  api(project(path = ":lib-1"))
        |  api(project(path = ":lib-2"))
        |  implementation(project(path = ":lib-3"))
        |}
        |
        |
        |""".trimMargin()
    }

    @Test
    fun `with autoCorrect and preceding typesafe external dependency should be commented out`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("kotlin(\"jvm\")")
            addProjectDependency("api", jvmSub1)
            addTypesafeExternalDependency("api", "libs.javax.inject")
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

      shouldSucceed("moduleCheckUnusedDependency") withTrimmedMessage """:app
           dependency    name                source    build file
        ✔  :lib-2        unusedDependency              /app/build.gradle.kts: (8, 3):
        ✔  :lib-3        unusedDependency              /app/build.gradle.kts: (9, 3):

ModuleCheck found 2 issues"""

      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  api(project(path = ":lib-1"))
        |  api(libs.javax.inject)
        |  // api(project(path = ":lib-2"))  // ModuleCheck finding [unusedDependency]
        |  // implementation(project(path = ":lib-3"))  // ModuleCheck finding [unusedDependency]
        |}
        |""".trimMargin()
    }

    @Test
    fun `with autoCorrect and deleteUnused should be deleted`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("kotlin(\"jvm\")")
            addProjectDependency("api", jvmSub1)
            addProjectDependency("api", jvmSub2)
            addProjectDependency("implementation", jvmSub3, "// this is a comment")
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
            |  deleteUnused = true
            |}
          """.trimMargin()
            ).build()
        )
      }
        .writeIn(testProjectDir.toPath())

      shouldSucceed("moduleCheckUnusedDependency") withTrimmedMessage """:app
           dependency    name                source    build file
        ✔  :lib-2        unusedDependency              /app/build.gradle.kts: (7, 3):
        ✔  :lib-3        unusedDependency              /app/build.gradle.kts: (9, 3):

ModuleCheck found 2 issues"""

      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  api(project(path = ":lib-1"))
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

      shouldSucceed("moduleCheckUnusedDependency") withTrimmedMessage """:app
           dependency    name                source    build file
        ✔  :lib-2        unusedDependency              /app/build.gradle.kts: (7, 3):
        ✔  :lib-3        unusedDependency              /app/build.gradle.kts: (8, 3):

ModuleCheck found 2 issues"""

      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  "api"(project(path = ":lib-1"))
        |  // "api"(project(path = ":lib-2"))  // ModuleCheck finding [unusedDependency]
        |  // "implementation"(project(path = ":lib-3"))  // ModuleCheck finding [unusedDependency]
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

      shouldFail("moduleCheckUnusedDependency") withTrimmedMessage """:app
           dependency    name                source    build file
        X  :lib-2        unusedDependency              /app/build.gradle.kts: (7, 3):
        X  :lib-3        unusedDependency              /app/build.gradle.kts: (8, 3):

ModuleCheck found 2 issues"""
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

      shouldFail("moduleCheckUnusedDependency") withTrimmedMessage """:app
           dependency    name                source    build file
        X  :lib-2        unusedDependency              /app/build.gradle.kts: (7, 3):
        X  :lib-3        unusedDependency              /app/build.gradle.kts: (8, 3):

ModuleCheck found 2 issues"""
    }

    @Test
    fun `with autoCorrect following dependency configuration block should be fixed`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("kotlin(\"jvm\")")
            addProjectDependency("api", jvmSub1, inlineComment = "{\n  }")
            addProjectDependency("api", jvmSub2)
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
        addSubprojects(jvmSub1, jvmSub2)
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

      shouldSucceed("moduleCheckUnusedDependency") withTrimmedMessage """:app
           dependency    name                source    build file
        ✔  :lib-2        unusedDependency              /app/build.gradle.kts: (8, 3):

ModuleCheck found 1 issues"""

      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  api(project(path = ":lib-1")) {
        |  }
        |  // api(project(path = ":lib-2"))  // ModuleCheck finding [unusedDependency]
        |}
        |""".trimMargin()
    }

    @Test
    fun `dependencies from non-jvm configuration should be ignored`() {
      val lib1 = ProjectSpec("lib1") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("kotlin(\"jvm\")")
            addProjectDependency("\"fakeConfig\"", jvmSub1)
          }
        )
      }
      ProjectSpec("project") {
        applyEach(projects) { project ->
          addSubproject(project)
        }
        addSubproject(lib1)
        addSubprojects(jvmSub1)
        addSettingsSpec(projectSettings.build())
        addBuildSpec(
          projectBuild
            .addBlock(
              """
        subprojects {
          configurations.create("fakeConfig")
        }
              """.trimIndent()
            ).build()
        )
      }
        .writeIn(testProjectDir.toPath())

      shouldSucceed("moduleCheckUnusedDependency")
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

      shouldSucceed("moduleCheckUnusedDependency")
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

      shouldSucceed("moduleCheckUnusedDependency")
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

      shouldSucceed("moduleCheckUnusedDependency")
    }

    @Test
    fun `module with a custom view used as a ViewBinding object in subject module should not be unused`() {

      val bindingClassName = ClassName("com.example.lib1.databinding", "ActivityMainBinding")

      val myActivity = FileSpec.builder("com.example.app", "MyActivity")
        .addProperty(PropertySpec.builder("binding", bindingClassName).build())
        .build()

      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""id("com.android.library")""")
            addPlugin("kotlin(\"android\")")
            android = true
            addProjectDependency("api", jvmSub1)
            addBlock("android.buildFeatures.viewBinding = true")
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/java")) {
            addFileSpec(myActivity)
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

      val activity_main_xml = RawFile(
        "activity_main.xml",
        """<?xml version="1.0" encoding="utf-8"?>
        |<androidx.constraintlayout.widget.ConstraintLayout
        |  xmlns:android="http://schemas.android.com/apk/res/android"
        |  android:id="@+id/fragment_container"
        |  android:layout_width="match_parent"
        |  android:layout_height="match_parent"
        |  />
        """.trimMargin()
      )

      ProjectSpec("project") {
        applyEach(projects) { project ->
          addSubproject(project)
        }
        addSubproject(appProject)
        addSubproject(
          ProjectSpec("lib-1") {
            addBuildSpec(
              ProjectBuildSpec {
                addPlugin("""id("com.android.library")""")
                addPlugin("kotlin(\"android\")")
                android = true
                addProjectDependency("api", jvmSub1)
                addBlock("android.buildFeatures.viewBinding = true")
              }
            )
            addSrcSpec(
              ProjectSrcSpec(Path.of("src/main/res/layout")) {
                addRawFile(activity_main_xml)
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

      shouldSucceed("moduleCheckUnusedDependency")
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
              "MyFile.kt",
              """
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
                addRawFile(
                  "Lib1View.kt",
                  """
                package com.example.lib1

                import com.squareup.anvil.annotations.ContributesMultibinding

                @ContributesMultibinding(AppScope)
                public class Lib1View
                  """.trimIndent()
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

      shouldSucceed("moduleCheckUnusedDependency")
    }

    @Test
    fun `module with a declaration in testFixtures used in dependent module should not be unused`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("kotlin(\"jvm\")")
            addRawDependency("""testImplementation(testFixtures(project(":lib-1")))""")
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/test/java")) {
            addRawFile(
              "MyFile.kt",
              """
            package com.example.app

            import com.example.lib1.Lib1View

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
            addBuildSpec(
              ProjectBuildSpec {
                addPlugin("kotlin(\"jvm\")")
                addPlugin("id(\"java-test-fixtures\")")
              }
            )
            addSrcSpec(
              ProjectSrcSpec(Path.of("src/testFixtures/java")) {
                addRawFile(
                  "Lib1View.kt",
                  """
                package com.example.lib1

                import com.squareup.anvil.annotations.ContributesMultibinding

                @ContributesMultibinding(AppScope)
                public class Lib1View
                  """.trimIndent()
                )
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

      shouldSucceed("moduleCheckUnusedDependency")
    }

    @Test
    fun `module with a declaration in testFixtures which is unused should be auto-corrected`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("kotlin(\"jvm\")")
            addRawDependency("""testImplementation(testFixtures(project(":lib-1")))""")
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
            addBuildSpec(
              ProjectBuildSpec {
                addPlugin("kotlin(\"jvm\")")
                addPlugin("id(\"java-test-fixtures\")")
              }
            )
            addSrcSpec(
              ProjectSrcSpec(Path.of("src/testFixtures/java")) {
                addRawFile(
                  "Lib1View.kt",
                  """
                package com.example.lib1

                import com.squareup.anvil.annotations.ContributesMultibinding

                @ContributesMultibinding(AppScope)
                public class Lib1View
                  """.trimIndent()
                )
              }
            )
          }
        )
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

      shouldSucceed("moduleCheckUnusedDependency") withTrimmedMessage """:app
           dependency    name                source    build file
        ✔  :lib-1        unusedDependency              /app/build.gradle.kts: (6, 3):

ModuleCheck found 1 issues"""

      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  // testImplementation(testFixtures(project(":lib-1")))  // ModuleCheck finding [unusedDependency]
        |}
        |""".trimMargin()
    }

    @Test
    fun `module with a declaration used in an android module with kotlin source directory should not be unused`() {
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
          ProjectSrcSpec(Path.of("src/main/kotlin")) {
            addRawFile(
              "MyModule.kt",
              """
            package com.example.app

            import com.example.lib1.AppScope
            import com.squareup.anvil.annotations.ContributesTo
            import dagger.Module

            @Module
            @ContributesTo(AppScope::class)
            object MyModule
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
                addRawFile(
                  "AppScope.kt",
                  """
                package com.example.lib1

                import javax.inject.Scope
                import kotlin.reflect.KClass

                abstract class AppScope private constructor()
                  """.trimIndent()
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

      shouldSucceed("moduleCheckUnusedDependency")
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
              "MyFile.kt",
              """
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

      shouldSucceed("moduleCheckUnusedDependency")
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

      shouldSucceed("moduleCheckUnusedDependency")
    }

    @Test
    fun `module with a string resource used in subject module's AndroidManifest should not be unused`() {

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
          ProjectSrcSpec(Path.of("src/main")) {
            addRawFile(
              RawFile(
                "AndroidManifest.xml",
                """<manifest
                |  xmlns:android="http://schemas.android.com/apk/res/android"
                |  package="com.example.app"
                |  >
                |
                |  <application
                |    android:name=".App"
                |    android:allowBackup="true"
                |    android:icon="@mipmap/ic_launcher"
                |    android:label="@string/app_name"
                |    android:roundIcon="@mipmap/ic_launcher_round"
                |    android:supportsRtl="true"
                |    android:theme="@style/AppTheme"
                |    />
                |</manifest>
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

      shouldSucceed("moduleCheckUnusedDependency")
    }

    @Test
    fun `module with an auto-generated manifest and a string resource used in subject module should not be unused`() {

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

        // without this, the standard manifest will be generated and this test won't be testing anything
        disableAutoManifest = true

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
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""id("com.android.library")""")
            addPlugin("kotlin(\"android\")")
            android = true
            // This reproduces the behavior of Auto-Manifest:
            // https://github.com/GradleUp/auto-manifest
            // For some reason, that plugin doesn't work with Gradle TestKit.  Its task is never
            // registered, and the manifest location is never changed from the default.  When I open
            // the generated project dir and execute the task from terminal, it works fine...
            // This does the same thing, but uses a different default directory.
            addBlock(
              """
          |val manifestFile = file("${'$'}buildDir/generated/my-custom-manifest-location/AndroidManifest.xml")
          |
          |android {
          |  sourceSets {
          |    findByName("main")?.manifest {
          |      srcFile(manifestFile.path)
          |    }
          |  }
          |}
          |
          |val makeFile by tasks.registering {
          |
          |  doFirst {
          |
          |    manifestFile.parentFile.mkdirs()
          |    manifestFile.writeText(
          |      ""${'"'}<manifest package="com.example.lib1" /> ""${'"'}.trimMargin()
          |    )
          |  }
          |}
          |
          |afterEvaluate {
          |
          |  tasks.withType(com.android.build.gradle.tasks.GenerateBuildConfig::class.java)
          |    .configureEach { dependsOn(makeFile) }
          |  tasks.withType(com.android.build.gradle.tasks.MergeResources::class.java)
          |    .configureEach { dependsOn(makeFile) }
          |  tasks.withType(com.android.build.gradle.tasks.ManifestProcessorTask::class.java)
          |    .configureEach { dependsOn(makeFile)}
          |
          |}
          """.trimMargin()
            )
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

      shouldSucceed("moduleCheckUnusedDependency")

      // one last check to make sure the manifest wasn't generated, since that would invalidate the test
      File(testProjectDir, "/lib1/src/main/AndroidManifest.xml").exists() shouldBe false
    }

    @Test
    fun `module with a declaration used via a class reference with wildcard import should not be unused`() {
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
              "MyModule.kt",
              """
            package com.example.app

            import com.example.lib1.*
            import com.squareup.anvil.annotations.ContributesTo
            import dagger.Module

            @Module
            @ContributesTo(AppScope::class)
            object MyModule
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
                addRawFile(
                  "AppScope.kt",
                  """
                package com.example.lib1

                import javax.inject.Scope
                import kotlin.reflect.KClass

                /**
                 * Maybe this should just be part of Anvil's api?
                 */
                abstract class AppScope private constructor()

                @Scope
                @Retention(AnnotationRetention.RUNTIME)
                annotation class SingleIn(val clazz: KClass<*>)
                  """.trimIndent()
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

      shouldSucceed("moduleCheckUnusedDependency")
    }
  }
}
