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
import modulecheck.specs.ProjectBuildSpec
import modulecheck.specs.ProjectSettingsSpec
import modulecheck.specs.ProjectSpec
import org.junit.jupiter.api.Test
import java.io.File

class SortDependenciesTest : BasePluginTest() {

  val projects = List(10) {
    jvmSubProject("lib-$it", ClassName("com.example.lib$it", "Lib${it}Class"))
  }

  @Test
  fun `configurations should be grouped and sorted`() {
    ProjectSpec("project") {
      addSettingsSpec(
        ProjectSettingsSpec {
          projects.forEach { project ->
            addInclude(project.gradlePath)
          }
          addInclude("app")
        }
      )
      addBuildSpec(
        ProjectBuildSpec {
          addPlugin("id(\"com.rickbusarow.module-check\")")
          buildScript()
        }
      )
      projects.forEach { project -> addSubproject(project) }
      addSubproject(
        ProjectSpec("app") {
          addBuildSpec(
            ProjectBuildSpec {
              addPlugin("kotlin(\"jvm\")")
              addProjectDependency("runtimeOnly", "lib-1")
              addProjectDependency("api", "lib-3")
              addProjectDependency("implementation", "lib-7")
              addProjectDependency("compileOnly", "lib-4")
              addProjectDependency("api", "lib-0")
              addProjectDependency("testImplementation", "lib-5")
              addProjectDependency("compileOnly", "lib-6")
              addProjectDependency("implementation", "lib-2")
              addProjectDependency("testImplementation", "lib-8")
              addProjectDependency("implementation", "lib-9")
            }
          )
        }
      )
    }
      .writeIn(testProjectDir.toPath())

    build("moduleCheckSortDependencies").shouldSucceed()

    File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  api(project(path = ":lib-0"))
        |  api(project(path = ":lib-3"))
        |
        |  compileOnly(project(path = ":lib-4"))
        |  compileOnly(project(path = ":lib-6"))
        |
        |  implementation(project(path = ":lib-2"))
        |  implementation(project(path = ":lib-7"))
        |  implementation(project(path = ":lib-9"))
        |
        |  runtimeOnly(project(path = ":lib-1"))
        |
        |  testImplementation(project(path = ":lib-5"))
        |  testImplementation(project(path = ":lib-8"))
        |}
        |""".trimMargin()
  }

  @Test
  fun `external dependencies should be grouped separately`() {
    ProjectSpec("project") {
      addSettingsSpec(
        ProjectSettingsSpec {
          projects.forEach { project ->
            addInclude(project.gradlePath)
          }
          addInclude("app")
        }
      )
      addBuildSpec(
        ProjectBuildSpec {
          addPlugin("id(\"com.rickbusarow.module-check\")")
          buildScript()
        }
      )
      projects.forEach { project -> addSubproject(project) }
      addSubproject(
        ProjectSpec("app") {
          addBuildSpec(
            ProjectBuildSpec {
              addPlugin("kotlin(\"jvm\")")
              addExternalDependency("api", "com.squareup:kotlinpoet:1.7.2")
              addProjectDependency("api", "lib-3")
              addProjectDependency("implementation", "lib-7")
              addExternalDependency(
                "implementation",
                "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2"
              )
              addProjectDependency("compileOnly", "lib-4")
              addProjectDependency("api", "lib-0")
              addProjectDependency("testImplementation", "lib-8")
              addExternalDependency(
                "testImplementation",
                "org.junit.jupiter:junit-jupiter-api:5.7.0"
              )
              addProjectDependency("implementation", "lib-9")
            }
          )
        }
      )
    }
      .writeIn(testProjectDir.toPath())

    build("moduleCheckSortDependencies").shouldSucceed()

    File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  api("com.squareup:kotlinpoet:1.7.2")
        |
        |  api(project(path = ":lib-0"))
        |  api(project(path = ":lib-3"))
        |
        |  compileOnly(project(path = ":lib-4"))
        |
        |  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
        |
        |  implementation(project(path = ":lib-7"))
        |  implementation(project(path = ":lib-9"))
        |
        |  testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
        |
        |  testImplementation(project(path = ":lib-8"))
        |}
        |""".trimMargin()
  }

  @Test
  fun `comments should move along with the dependency declaration`() {
    ProjectSpec("project") {
      addSettingsSpec(
        ProjectSettingsSpec {
          projects.forEach { project ->
            addInclude(project.gradlePath)
          }
          addInclude("app")
        }
      )
      addBuildSpec(
        ProjectBuildSpec {
          addPlugin("id(\"com.rickbusarow.module-check\")")
          buildScript()
        }
      )
      projects.forEach { project -> addSubproject(project) }
      addSubproject(
        ProjectSpec("app") {
          addBuildSpec(
            ProjectBuildSpec {
              addPlugin("kotlin(\"jvm\")")
              addExternalDependency(
                "api",
                "com.squareup:kotlinpoet:1.7.2",
                "// multi-line\n  // comment"
              )
              addRawDependency("/*\n  block comment\n  */\n  api(project(path = \":lib-3\"))")
              addProjectDependency("implementation", "lib-7", "/**\n  * block comment\n  */")
              addExternalDependency(
                "implementation",
                "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2"
              )
              addProjectDependency("compileOnly", "lib-4")
              addProjectDependency("api", "lib-0")
              addProjectDependency("testImplementation", "lib-5")
              addProjectDependency("compileOnly", "lib-6", "// floating comment\n\n")
              addProjectDependency("implementation", "lib-2", "// library 2")
              addProjectDependency("testImplementation", "lib-8")
              addExternalDependency(
                "testImplementation",
                "org.junit.jupiter:junit-jupiter-api:5.7.0",
                inlineComment = "// JUnit 5"
              )
              addProjectDependency("implementation", "lib-9", "// library 9")
            }
          )
        }
      )
    }
      .writeIn(testProjectDir.toPath())

    build("moduleCheckSortDependencies").shouldSucceed()

    File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  // multi-line
        |  // comment
        |  api("com.squareup:kotlinpoet:1.7.2")
        |
        |  api(project(path = ":lib-0"))
        |  /*
        |  block comment
        |  */
        |  api(project(path = ":lib-3"))
        |
        |  compileOnly(project(path = ":lib-4"))
        |  // floating comment
        |
        |
        |  compileOnly(project(path = ":lib-6"))
        |
        |  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
        |
        |  // library 2
        |  implementation(project(path = ":lib-2"))
        |  /**
        |  * block comment
        |  */
        |  implementation(project(path = ":lib-7"))
        |  // library 9
        |  implementation(project(path = ":lib-9"))
        |
        |  testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0") // JUnit 5
        |
        |  testImplementation(project(path = ":lib-5"))
        |  testImplementation(project(path = ":lib-8"))
        |}
        |""".trimMargin()
  }

  @Test
  fun `sorting should be idempotent`() {
    ProjectSpec("project") {
      addSettingsSpec(
        ProjectSettingsSpec {
          projects.forEach { project ->
            addInclude(project.gradlePath)
          }
          addInclude("app")
        }
      )
      addBuildSpec(
        ProjectBuildSpec {
          addPlugin("id(\"com.rickbusarow.module-check\")")
          buildScript()
        }
      )
      projects.forEach { project -> addSubproject(project) }
      addSubproject(
        ProjectSpec("app") {
          addBuildSpec(
            ProjectBuildSpec {
              addPlugin("kotlin(\"jvm\")")
              addExternalDependency(
                "api",
                "com.squareup:kotlinpoet:1.7.2",
                "// multi-line\n  // comment"
              )
              addRawDependency("/*\n  block comment\n  */\n  api(project(path = \":lib-3\"))")
              addProjectDependency("implementation", "lib-7", "/**\n  * block comment\n  */")
              addExternalDependency(
                "implementation",
                "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2"
              )
              addProjectDependency("compileOnly", "lib-4")
              addProjectDependency("api", "lib-0")
              addProjectDependency("testImplementation", "lib-5")
              addProjectDependency("compileOnly", "lib-6", "// floating comment\n\n")
              addProjectDependency("implementation", "lib-2", "// library 2")
              addProjectDependency("testImplementation", "lib-8")
              addExternalDependency(
                "testImplementation",
                "org.junit.jupiter:junit-jupiter-api:5.7.0",
                inlineComment = "// JUnit 5"
              )
              addProjectDependency("implementation", "lib-9", "// library 9")
            }
          )
        }
      )
    }
      .writeIn(testProjectDir.toPath())

    build("moduleCheckSortDependencies").shouldSucceed()
    build("moduleCheckSortDependencies").shouldSucceed()

    File(testProjectDir, "/app/build.gradle.kts").readText() shouldBe """plugins {
        |  kotlin("jvm")
        |}
        |
        |dependencies {
        |  // multi-line
        |  // comment
        |  api("com.squareup:kotlinpoet:1.7.2")
        |
        |  api(project(path = ":lib-0"))
        |  /*
        |  block comment
        |  */
        |  api(project(path = ":lib-3"))
        |
        |  compileOnly(project(path = ":lib-4"))
        |  // floating comment
        |
        |
        |  compileOnly(project(path = ":lib-6"))
        |
        |  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
        |
        |  // library 2
        |  implementation(project(path = ":lib-2"))
        |  /**
        |  * block comment
        |  */
        |  implementation(project(path = ":lib-7"))
        |  // library 9
        |  implementation(project(path = ":lib-9"))
        |
        |  testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0") // JUnit 5
        |
        |  testImplementation(project(path = ":lib-5"))
        |  testImplementation(project(path = ":lib-8"))
        |}
        |""".trimMargin()
  }

  @Test
  fun `buildScript dependencies should not be sorted`() {
    ProjectSpec("project") {
      addSettingsSpec(
        ProjectSettingsSpec {
          projects.forEach { project ->
            addInclude(project.gradlePath)
          }
          addInclude("app")
        }
      )
      addBuildSpec(
        ProjectBuildSpec {
          addPlugin("id(\"com.rickbusarow.module-check\")")
          buildScript()
        }
      )
      projects.forEach { project -> addSubproject(project) }
    }
      .writeIn(testProjectDir.toPath())

    build("moduleCheckSortDependencies").shouldSucceed()

    File(testProjectDir, "/build.gradle.kts").readText() shouldBe """
        |buildscript {
        |  repositories {
        |    mavenCentral()
        |    google()
        |    jcenter()
        |    maven("https://plugins.gradle.org/m2/")
        |    maven("https://oss.sonatype.org/content/repositories/snapshots")
        |  }
        |  dependencies {
        |    classpath("com.android.tools.build:gradle:7.0.2")
        |    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.30")
        |    classpath("com.squareup.anvil:gradle-plugin:2.3.4")
        |  }
        |}
        |
        |allprojects {
        |
        |  repositories {
        |    mavenCentral()
        |    google()
        |    jcenter()
        |    maven("https://plugins.gradle.org/m2/")
        |    maven("https://oss.sonatype.org/content/repositories/snapshots")
        |  }
        |
        |}
        |plugins {
        |  id("com.rickbusarow.module-check")
        |}
        |
        |""".trimMargin()
  }
}
