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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import modulecheck.specs.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

class MustBeApiTest : BasePluginTest() {

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
  inner class `with auto-correct` {

    @Test
    fun `single implementation property should be switched to api and succeed`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub4)
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

      shouldSucceed(
        "moduleCheckMustBeApi",
        "moduleCheckSortDependencies"
      )

      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  api(project(path = ":lib-4"))
        |}
        |""".trimMargin()
    }

    @Test
    fun `multiple implementation properties should be switched to api and succeed`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub1)
            addProjectDependency("implementation", jvmSub2)
            addProjectDependency("implementation", jvmSub3)
            addProjectDependency("implementation", jvmSub4)
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

      shouldSucceed(
        "moduleCheckMustBeApi",
        "moduleCheckSortDependencies"
      )

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

    @Test
    fun `single implementation supertype should be switched to api and succeed`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub4)
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/java")) {
            addFileSpec(
              FileSpec.builder("com.example.app", "MyApp")
                .addType(
                  TypeSpec.classBuilder("MyApp")
                    .addSuperinterface(lib4ClassName)
                    .build()
                ).build()
            )
          }
        )
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

      shouldSucceed(
        "moduleCheckMustBeApi",
        "moduleCheckSortDependencies"
      )

      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  api(project(path = ":lib-4"))
        |}
        |""".trimMargin()
    }

    @Test
    fun `multiple implementation supertypes should be switched to api and succeed`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub1)
            addProjectDependency("implementation", jvmSub2)
            addProjectDependency("implementation", jvmSub3)
            addProjectDependency("implementation", jvmSub4)
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/java")) {
            addFileSpec(
              FileSpec.builder("com.example.app", "MyApp")
                .addType(
                  TypeSpec.classBuilder("MyApp")
                    .addSuperinterface(lib1ClassName)
                    .addSuperinterface(lib2ClassName)
                    .addSuperinterface(lib3ClassName)
                    .addSuperinterface(lib4ClassName)
                    .build()
                ).build()
            )
          }
        )
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

      shouldSucceed(
        "moduleCheckMustBeApi",
        "moduleCheckSortDependencies"
      )

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

    @Test
    fun `single implementation return type should be switched to api and succeed`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub4)
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/java")) {
            addFileSpec(
              FileSpec.builder("com.example.app", "MyApp")
                .addFunction(
                  FunSpec.builder("create4")
                    .returns(lib4ClassName)
                    .addCode("return %T()", lib4ClassName)
                    .build()
                ).build()
            )
          }
        )
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

      shouldSucceed(
        "moduleCheckMustBeApi",
        "moduleCheckSortDependencies"
      )

      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  api(project(path = ":lib-4"))
        |}
        |""".trimMargin()
    }

    @Test
    fun `multiple implementation return types should be switched to api and succeed`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub1)
            addProjectDependency("implementation", jvmSub2)
            addProjectDependency("implementation", jvmSub3)
            addProjectDependency("implementation", jvmSub4)
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/java")) {
            addFileSpec(
              FileSpec.builder("com.example.app", "MyApp")
                .addFunction(
                  FunSpec.builder("create1")
                    .returns(lib1ClassName)
                    .addCode("return %T()", lib1ClassName)
                    .build()
                )
                .addFunction(
                  FunSpec.builder("create2")
                    .returns(lib2ClassName)
                    .addCode("return %T()", lib2ClassName)
                    .build()
                )
                .addFunction(
                  FunSpec.builder("create3")
                    .returns(lib3ClassName)
                    .addCode("return %T()", lib3ClassName)
                    .build()
                )
                .addFunction(
                  FunSpec.builder("create4")
                    .returns(lib4ClassName)
                    .addCode("return %T()", lib4ClassName)
                    .build()
                ).build()
            )
          }
        )
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

      shouldSucceed(
        "moduleCheckMustBeApi",
        "moduleCheckSortDependencies"
      )

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

    @Test
    fun `single implementation parameter should be switched to api and succeed`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub4)
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/java")) {
            addFileSpec(
              FileSpec.builder("com.example.app", "MyApp")
                .addFunction(
                  FunSpec.builder("print4")
                    .addParameter(ParameterSpec("lib4Class", lib4ClassName))
                    .addCode("println(lib4Class)")
                    .build()
                ).build()
            )
          }
        )
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

      shouldSucceed(
        "moduleCheckMustBeApi",
        "moduleCheckSortDependencies"
      )

      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  api(project(path = ":lib-4"))
        |}
        |""".trimMargin()
    }

    @Test
    fun `multiple implementation parameters should be switched to api and succeed`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub1)
            addProjectDependency("implementation", jvmSub2)
            addProjectDependency("implementation", jvmSub3)
            addProjectDependency("implementation", jvmSub4)
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/java")) {
            addFileSpec(
              FileSpec.builder("com.example.app", "MyApp")
                .addFunction(
                  FunSpec.builder("print1")
                    .addParameter(ParameterSpec("lib1Class", lib1ClassName))
                    .addCode("println(lib1Class)")
                    .build()
                )
                .addFunction(
                  FunSpec.builder("print2")
                    .addParameter(ParameterSpec("lib2Class", lib2ClassName))
                    .addCode("println(lib2Class)")
                    .build()
                )
                .addFunction(
                  FunSpec.builder("print3")
                    .addParameter(ParameterSpec("lib3Class", lib3ClassName))
                    .addCode("println(lib3Class)")
                    .build()
                )
                .addFunction(
                  FunSpec.builder("print4")
                    .addParameter(ParameterSpec("lib4Class", lib4ClassName))
                    .addCode("println(lib4Class)")
                    .build()
                ).build()
            )
          }
        )
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

      shouldSucceed(
        "moduleCheckMustBeApi",
        "moduleCheckSortDependencies"
      )

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

    @Test
    fun `single implementation generic parameter should be switched to api and succeed`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub4)
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/java")) {
            addFileSpec(
              FileSpec.builder("com.example.app", "MyApp")
                .addFunction(
                  FunSpec.builder("print4")
                    .addParameter(
                      ParameterSpec(
                        "lib4Class",
                        List::class.asClassName().parameterizedBy(lib4ClassName)
                      )
                    )
                    .addCode("println(lib4Class)")
                    .build()
                ).build()
            )
          }
        )
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

      shouldSucceed(
        "moduleCheckMustBeApi",
        "moduleCheckSortDependencies"
      )

      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  api(project(path = ":lib-4"))
        |}
        |""".trimMargin()
    }

    @Test
    fun `multiple implementation generic parameters should be switched to api and succeed`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub1)
            addProjectDependency("implementation", jvmSub2)
            addProjectDependency("implementation", jvmSub3)
            addProjectDependency("implementation", jvmSub4)
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/java")) {
            addFileSpec(
              FileSpec.builder("com.example.app", "MyApp")
                .addFunction(
                  FunSpec.builder("print1")
                    .addParameter(
                      ParameterSpec(
                        "lib1Class",
                        List::class.asClassName().parameterizedBy(lib1ClassName)
                      )
                    )
                    .addCode("println(lib1Class)")
                    .build()
                )
                .addFunction(
                  FunSpec.builder("print2")
                    .addParameter(
                      ParameterSpec(
                        "lib2Class",
                        List::class.asClassName().parameterizedBy(lib2ClassName)
                      )
                    )
                    .addCode("println(lib2Class)")
                    .build()
                )
                .addFunction(
                  FunSpec.builder("print3")
                    .addParameter(
                      ParameterSpec(
                        "lib3Class",
                        List::class.asClassName().parameterizedBy(lib3ClassName)
                      )
                    )
                    .addCode("println(lib3Class)")
                    .build()
                )
                .addFunction(
                  FunSpec.builder("print4")
                    .addParameter(
                      ParameterSpec(
                        "lib4Class",
                        List::class.asClassName().parameterizedBy(lib4ClassName)
                      )
                    )
                    .addCode("println(lib4Class)")
                    .build()
                ).build()
            )
          }
        )
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

      shouldSucceed(
        "moduleCheckMustBeApi",
        "moduleCheckSortDependencies"
      )

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

    @Test
    fun `single implementation generic parameter bound should be switched to api and succeed`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub4)
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/java")) {
            addFileSpec(
              FileSpec.builder("com.example.app", "MyApp")
                .addFunction(
                  FunSpec.builder("print4")
                    .addTypeVariable(TypeVariableName("T", lib4ClassName))
                    .addParameter(
                      ParameterSpec(
                        "lib4Class",
                        TypeVariableName("T")
                      )
                    )
                    .addCode("println(lib4Class)")
                    .build()
                ).build()
            )
          }
        )
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

      shouldSucceed(
        "moduleCheckMustBeApi",
        "moduleCheckSortDependencies"
      )

      File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  api(project(path = ":lib-4"))
        |}
        |""".trimMargin()
    }

    @Test
    fun `multiple implementation generic parameters bound should be switched to api and succeed`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub1)
            addProjectDependency("implementation", jvmSub2)
            addProjectDependency("implementation", jvmSub3)
            addProjectDependency("implementation", jvmSub4)
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/java")) {
            addFileSpec(
              FileSpec.builder("com.example.app", "MyApp")
                .addFunction(
                  FunSpec.builder("print1")
                    .addTypeVariable(TypeVariableName("T", lib1ClassName))
                    .addParameter(
                      ParameterSpec(
                        "lib1Class",
                        TypeVariableName("T")
                      )
                    )
                    .addCode("println(lib1Class)")
                    .build()
                )
                .addFunction(
                  FunSpec.builder("print2")
                    .addTypeVariable(TypeVariableName("T", lib2ClassName))
                    .addParameter(
                      ParameterSpec(
                        "lib2Class",
                        TypeVariableName("T")
                      )
                    )
                    .addCode("println(lib2Class)")
                    .build()
                )
                .addFunction(
                  FunSpec.builder("print3")
                    .addTypeVariable(TypeVariableName("T", lib3ClassName))
                    .addParameter(
                      ParameterSpec(
                        "lib3Class",
                        TypeVariableName("T")
                      )
                    )
                    .addCode("println(lib3Class)")
                    .build()
                )
                .addFunction(
                  FunSpec.builder("print4")
                    .addTypeVariable(TypeVariableName("T", lib4ClassName))
                    .addParameter(
                      ParameterSpec(
                        "lib4Class",
                        TypeVariableName("T")
                      )
                    )
                    .addCode("println(lib4Class)")
                    .build()
                ).build()
            )
          }
        )
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

      shouldSucceed(
        "moduleCheckMustBeApi",
        "moduleCheckSortDependencies"
      )

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
    fun `single implementation property should fail with message`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub4)
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

      shouldFail("moduleCheckMustBeApi") withTrimmedMessage """:app
           dependency    name         source    build file
        X  :lib-4        mustBeApi              /app/build.gradle.kts: (6, 3):

ModuleCheck found 1 issues"""
    }

    @Test
    fun `single implementation property which is suppressed should succeed`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub4, "@Suppress(\"mustBeApi\")")
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

      shouldSucceed("moduleCheckMustBeApi")
    }

    @Test
    fun `multiple implementation properties should fail with message`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub1)
            addProjectDependency("implementation", jvmSub2)
            addProjectDependency("implementation", jvmSub3)
            addProjectDependency("implementation", jvmSub4)
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

      shouldFail("moduleCheckMustBeApi") withTrimmedMessage """:app
           dependency    name         source    build file
        X  :lib-1        mustBeApi              /app/build.gradle.kts: (6, 3):
        X  :lib-2        mustBeApi              /app/build.gradle.kts: (7, 3):
        X  :lib-3        mustBeApi              /app/build.gradle.kts: (8, 3):
        X  :lib-4        mustBeApi              /app/build.gradle.kts: (9, 3):

ModuleCheck found 4 issues"""
    }

    @Test
    fun `single implementation supertype should fail with message`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub4)
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/java")) {
            addFileSpec(
              FileSpec.builder("com.example.app", "MyApp")
                .addType(
                  TypeSpec.classBuilder("MyApp")
                    .addSuperinterface(lib4ClassName)
                    .build()
                ).build()
            )
          }
        )
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

      shouldFail("moduleCheckMustBeApi") withTrimmedMessage """:app
           dependency    name         source    build file
        X  :lib-4        mustBeApi              /app/build.gradle.kts: (6, 3):

ModuleCheck found 1 issues"""
    }

    @Test
    fun `multiple implementation supertypes should fail with message`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub1)
            addProjectDependency("implementation", jvmSub2)
            addProjectDependency("implementation", jvmSub3)
            addProjectDependency("implementation", jvmSub4)
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/java")) {
            addFileSpec(
              FileSpec.builder("com.example.app", "MyApp")
                .addType(
                  TypeSpec.classBuilder("MyApp")
                    .addSuperinterface(lib1ClassName)
                    .addSuperinterface(lib2ClassName)
                    .addSuperinterface(lib3ClassName)
                    .addSuperinterface(lib4ClassName)
                    .build()
                ).build()
            )
          }
        )
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

      shouldFail("moduleCheckMustBeApi") withTrimmedMessage """:app
           dependency    name         source    build file
        X  :lib-1        mustBeApi              /app/build.gradle.kts: (6, 3):
        X  :lib-2        mustBeApi              /app/build.gradle.kts: (7, 3):
        X  :lib-3        mustBeApi              /app/build.gradle.kts: (8, 3):
        X  :lib-4        mustBeApi              /app/build.gradle.kts: (9, 3):

ModuleCheck found 4 issues"""
    }

    @Test
    fun `single implementation return type should fail with message`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub4)
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/java")) {
            addFileSpec(
              FileSpec.builder("com.example.app", "MyApp")
                .addFunction(
                  FunSpec.builder("create4")
                    .returns(lib4ClassName)
                    .addCode("return %T()", lib4ClassName)
                    .build()
                ).build()
            )
          }
        )
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

      shouldFail("moduleCheckMustBeApi") withTrimmedMessage """:app
           dependency    name         source    build file
        X  :lib-4        mustBeApi              /app/build.gradle.kts: (6, 3):

ModuleCheck found 1 issues"""
    }

    @Test
    fun `multiple implementation return types should fail with message`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub1)
            addProjectDependency("implementation", jvmSub2)
            addProjectDependency("implementation", jvmSub3)
            addProjectDependency("implementation", jvmSub4)
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/java")) {
            addFileSpec(
              FileSpec.builder("com.example.app", "MyApp")
                .addFunction(
                  FunSpec.builder("create1")
                    .returns(lib1ClassName)
                    .addCode("return %T()", lib1ClassName)
                    .build()
                )
                .addFunction(
                  FunSpec.builder("create2")
                    .returns(lib2ClassName)
                    .addCode("return %T()", lib2ClassName)
                    .build()
                )
                .addFunction(
                  FunSpec.builder("create3")
                    .returns(lib3ClassName)
                    .addCode("return %T()", lib3ClassName)
                    .build()
                )
                .addFunction(
                  FunSpec.builder("create4")
                    .returns(lib4ClassName)
                    .addCode("return %T()", lib4ClassName)
                    .build()
                ).build()
            )
          }
        )
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

      shouldFail("moduleCheckMustBeApi") withTrimmedMessage """:app
           dependency    name         source    build file
        X  :lib-1        mustBeApi              /app/build.gradle.kts: (6, 3):
        X  :lib-2        mustBeApi              /app/build.gradle.kts: (7, 3):
        X  :lib-3        mustBeApi              /app/build.gradle.kts: (8, 3):
        X  :lib-4        mustBeApi              /app/build.gradle.kts: (9, 3):

ModuleCheck found 4 issues"""
    }

    @Test
    fun `single implementation parameter should fail with message`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub4)
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/java")) {
            addFileSpec(
              FileSpec.builder("com.example.app", "MyApp")
                .addFunction(
                  FunSpec.builder("print4")
                    .addParameter(ParameterSpec("lib4Class", lib4ClassName))
                    .addCode("println(lib4Class)")
                    .build()
                ).build()
            )
          }
        )
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

      shouldFail("moduleCheckMustBeApi") withTrimmedMessage """:app
           dependency    name         source    build file
        X  :lib-4        mustBeApi              /app/build.gradle.kts: (6, 3):

ModuleCheck found 1 issues"""
    }

    @Test
    fun `multiple implementation parameters should fail with message`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub1)
            addProjectDependency("implementation", jvmSub2)
            addProjectDependency("implementation", jvmSub3)
            addProjectDependency("implementation", jvmSub4)
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/java")) {
            addFileSpec(
              FileSpec.builder("com.example.app", "MyApp")
                .addFunction(
                  FunSpec.builder("print1")
                    .addParameter(ParameterSpec("lib1Class", lib1ClassName))
                    .addCode("println(lib1Class)")
                    .build()
                )
                .addFunction(
                  FunSpec.builder("print2")
                    .addParameter(ParameterSpec("lib2Class", lib2ClassName))
                    .addCode("println(lib2Class)")
                    .build()
                )
                .addFunction(
                  FunSpec.builder("print3")
                    .addParameter(ParameterSpec("lib3Class", lib3ClassName))
                    .addCode("println(lib3Class)")
                    .build()
                )
                .addFunction(
                  FunSpec.builder("print4")
                    .addParameter(ParameterSpec("lib4Class", lib4ClassName))
                    .addCode("println(lib4Class)")
                    .build()
                ).build()
            )
          }
        )
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

      shouldFail("moduleCheckMustBeApi") withTrimmedMessage """:app
           dependency    name         source    build file
        X  :lib-1        mustBeApi              /app/build.gradle.kts: (6, 3):
        X  :lib-2        mustBeApi              /app/build.gradle.kts: (7, 3):
        X  :lib-3        mustBeApi              /app/build.gradle.kts: (8, 3):
        X  :lib-4        mustBeApi              /app/build.gradle.kts: (9, 3):

ModuleCheck found 4 issues"""
    }

    @Test
    fun `single implementation generic parameter should fail with message`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub4)
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/java")) {
            addFileSpec(
              FileSpec.builder("com.example.app", "MyApp")
                .addFunction(
                  FunSpec.builder("print4")
                    .addParameter(
                      ParameterSpec(
                        "lib4Class",
                        List::class.asClassName().parameterizedBy(lib4ClassName)
                      )
                    )
                    .addCode("println(lib4Class)")
                    .build()
                ).build()
            )
          }
        )
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

      shouldFail("moduleCheckMustBeApi") withTrimmedMessage """:app
           dependency    name         source    build file
        X  :lib-4        mustBeApi              /app/build.gradle.kts: (6, 3):

ModuleCheck found 1 issues"""
    }

    @Test
    fun `multiple implementation generic parameters should fail with message`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub1)
            addProjectDependency("implementation", jvmSub2)
            addProjectDependency("implementation", jvmSub3)
            addProjectDependency("implementation", jvmSub4)
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/java")) {
            addFileSpec(
              FileSpec.builder("com.example.app", "MyApp")
                .addFunction(
                  FunSpec.builder("print1")
                    .addParameter(
                      ParameterSpec(
                        "lib1Class",
                        List::class.asClassName().parameterizedBy(lib1ClassName)
                      )
                    )
                    .addCode("println(lib1Class)")
                    .build()
                )
                .addFunction(
                  FunSpec.builder("print2")
                    .addParameter(
                      ParameterSpec(
                        "lib2Class",
                        List::class.asClassName().parameterizedBy(lib2ClassName)
                      )
                    )
                    .addCode("println(lib2Class)")
                    .build()
                )
                .addFunction(
                  FunSpec.builder("print3")
                    .addParameter(
                      ParameterSpec(
                        "lib3Class",
                        List::class.asClassName().parameterizedBy(lib3ClassName)
                      )
                    )
                    .addCode("println(lib3Class)")
                    .build()
                )
                .addFunction(
                  FunSpec.builder("print4")
                    .addParameter(
                      ParameterSpec(
                        "lib4Class",
                        List::class.asClassName().parameterizedBy(lib4ClassName)
                      )
                    )
                    .addCode("println(lib4Class)")
                    .build()
                ).build()
            )
          }
        )
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

      shouldFail("moduleCheckMustBeApi") withTrimmedMessage """:app
           dependency    name         source    build file
        X  :lib-1        mustBeApi              /app/build.gradle.kts: (6, 3):
        X  :lib-2        mustBeApi              /app/build.gradle.kts: (7, 3):
        X  :lib-3        mustBeApi              /app/build.gradle.kts: (8, 3):
        X  :lib-4        mustBeApi              /app/build.gradle.kts: (9, 3):

ModuleCheck found 4 issues"""
    }

    @Test
    fun `single implementation generic parameter bound should fail with message`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub4)
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/java")) {
            addFileSpec(
              FileSpec.builder("com.example.app", "MyApp")
                .addFunction(
                  FunSpec.builder("print4")
                    .addTypeVariable(TypeVariableName("T", lib4ClassName))
                    .addParameter(
                      ParameterSpec(
                        "lib4Class",
                        TypeVariableName("T")
                      )
                    )
                    .addCode("println(lib4Class)")
                    .build()
                ).build()
            )
          }
        )
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

      shouldFail("moduleCheckMustBeApi") withTrimmedMessage """:app
           dependency    name         source    build file
        X  :lib-4        mustBeApi              /app/build.gradle.kts: (6, 3):

ModuleCheck found 1 issues"""
    }

    @Test
    fun `multiple implementation generic parameters bound should fail with message`() {
      val appProject = ProjectSpec("app") {
        addBuildSpec(
          ProjectBuildSpec {
            addPlugin("""kotlin("jvm")""")
            addProjectDependency("implementation", jvmSub1)
            addProjectDependency("implementation", jvmSub2)
            addProjectDependency("implementation", jvmSub3)
            addProjectDependency("implementation", jvmSub4)
          }
        )
        addSrcSpec(
          ProjectSrcSpec(Path.of("src/main/java")) {
            addFileSpec(
              FileSpec.builder("com.example.app", "MyApp")
                .addFunction(
                  FunSpec.builder("print1")
                    .addTypeVariable(TypeVariableName("T", lib1ClassName))
                    .addParameter(
                      ParameterSpec(
                        "lib1Class",
                        TypeVariableName("T")
                      )
                    )
                    .addCode("println(lib1Class)")
                    .build()
                )
                .addFunction(
                  FunSpec.builder("print2")
                    .addTypeVariable(TypeVariableName("T", lib2ClassName))
                    .addParameter(
                      ParameterSpec(
                        "lib2Class",
                        TypeVariableName("T")
                      )
                    )
                    .addCode("println(lib2Class)")
                    .build()
                )
                .addFunction(
                  FunSpec.builder("print3")
                    .addTypeVariable(TypeVariableName("T", lib3ClassName))
                    .addParameter(
                      ParameterSpec(
                        "lib3Class",
                        TypeVariableName("T")
                      )
                    )
                    .addCode("println(lib3Class)")
                    .build()
                )
                .addFunction(
                  FunSpec.builder("print4")
                    .addTypeVariable(TypeVariableName("T", lib4ClassName))
                    .addParameter(
                      ParameterSpec(
                        "lib4Class",
                        TypeVariableName("T")
                      )
                    )
                    .addCode("println(lib4Class)")
                    .build()
                ).build()
            )
          }
        )
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

      shouldFail("moduleCheckMustBeApi") withTrimmedMessage """:app
           dependency    name         source    build file
        X  :lib-1        mustBeApi              /app/build.gradle.kts: (6, 3):
        X  :lib-2        mustBeApi              /app/build.gradle.kts: (7, 3):
        X  :lib-3        mustBeApi              /app/build.gradle.kts: (8, 3):
        X  :lib-4        mustBeApi              /app/build.gradle.kts: (9, 3):

ModuleCheck found 4 issues"""
    }
  }
}
