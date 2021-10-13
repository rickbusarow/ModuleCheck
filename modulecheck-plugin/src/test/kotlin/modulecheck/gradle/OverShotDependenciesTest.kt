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
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import modulecheck.specs.*
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

class OverShotDependenciesTest : BasePluginTest() {

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

  @Test
  fun `jvm unused in main but used in test should be overshot`() {

    val myClass = FileSpec.builder("com.example.app", "MyClass")
      .addType(
        TypeSpec.classBuilder("MyClass")
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .addParameter("lib1Class", lib1ClassName)
              .build()
          )
          .build()
      )
      .build()

    val appProject = ProjectSpec("app") {
      addBuildSpec(
        ProjectBuildSpec {
          addPlugin("kotlin(\"jvm\")")
          addProjectDependency("api", jvmSub1)
        }
      )
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/test/java")) {
          addFileSpec(myClass)
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
          |  autoCorrect = true
          |}
        """.trimMargin()
          ).build()
      )
    }
      .writeIn(testProjectDir.toPath())

    build("moduleCheck").shouldSucceed()

    File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
      |  kotlin("jvm")
      |}
      |
      |dependencies {
      |  // api(project(path = ":lib-1"))  // ModuleCheck finding [unused]
      |  testImplementation(project(path = ":lib-1"))
      |}
      |""".trimMargin()
  }

  @Test
  fun `android unused in main but used in test should be overshot`() {

    val myClass = FileSpec.builder("com.example.app", "MyClass")
      .addType(
        TypeSpec.classBuilder("MyClass")
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .addParameter("lib1Class", lib1ClassName)
              .build()
          )
          .build()
      )
      .build()

    val appProject = ProjectSpec("app") {
      addBuildSpec(
        ProjectBuildSpec {
          android = true
          addPlugin("""id("com.android.library")""")
          addPlugin("kotlin(\"android\")")
          addProjectDependency("api", jvmSub1)
        }
      )
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/androidTest/java")) {
          addFileSpec(myClass)
        }
      )
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/test/java")) {
          addFileSpec(myClass)
        }
      )
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/debug/java")) {
          addFileSpec(myClass)
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
          |  autoCorrect = true
          |}
        """.trimMargin()
          ).build()
      )
    }
      .writeIn(testProjectDir.toPath())

    build("moduleCheck").shouldSucceed()

    File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
      |  id("com.android.library")
      |  kotlin("android")
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
      |  // api(project(path = ":lib-1"))  // ModuleCheck finding [unused]
      |  androidTestImplementation(project(path = ":lib-1"))
      |  debugImplementation(project(path = ":lib-1"))
      |  testImplementation(project(path = ":lib-1"))
      |}
      |""".trimMargin()
  }
}
