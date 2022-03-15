/*
 * Copyright (C) 2021-2022 Rick Busarow
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
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import modulecheck.specs.ProjectBuildSpec
import modulecheck.specs.ProjectBuildSpecBuilder
import modulecheck.specs.ProjectSettingsSpecBuilder
import modulecheck.specs.ProjectSpec
import modulecheck.specs.ProjectSrcSpec
import modulecheck.specs.applyEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

class UnusedDependenciesPluginTest : BasePluginTest() {

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
          addKotlinFile(
            "MyModule.kt",
            """
            package com.example.app

            import com.example.lib1.AppScope
            import com.squareup.anvil.annotations.ContributesTo
            import dagger.Module

            @Module
            @ContributesTo(AppScope::class)
            object MyModule
            """
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
              addKotlinFile(
                "AppScope.kt",
                """
                package com.example.lib1

                import javax.inject.Scope
                import kotlin.reflect.KClass

                abstract class AppScope private constructor()
                """
              )
            }
          )
          addBuildSpec(
            ProjectBuildSpec {
              addPlugin("kotlin(\"jvm\")")
              // addProjectDependency("api", jvmSub1)
            }
          )
        }
      )
      addSettingsSpec(projectSettings.build())
      addBuildSpec(projectBuild.build())
    }
      .writeIn(testProjectDir.toPath())

    shouldSucceed("moduleCheck")
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
          addXmlFile(
            "AndroidManifest.xml",
            """
              <manifest package="com.example.app" />
            """
          )
        }
      )
    }

    val androidSub1 = ProjectSpec("lib-1") {

      // without this, the standard manifest will be generated and this test won't be testing anything
      disableAutoManifest = true

      addSrcSpec(
        ProjectSrcSpec(Path.of("src/main/res/values")) {
          addXmlFile(
            "strings.xml",
            """
              <resources>
                <string name="app_name" translatable="false">MyApp</string>
              </resources>
            """
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
          val manifestFile = file("${'$'}buildDir/generated/my-custom-manifest-location/AndroidManifest.xml")

          android {
            sourceSets {
              findByName("main")?.manifest {
                srcFile(manifestFile.path)
              }
            }
          }

          val makeFile by tasks.registering {

            doFirst {

              manifestFile.parentFile.mkdirs()
              manifestFile.writeText(
                ""${'"'}<manifest package="com.example.lib1" /> ""${'"'}.trimMargin()
              )
            }
          }

          afterEvaluate {

            tasks.withType(com.android.build.gradle.tasks.GenerateBuildConfig::class.java)
              .configureEach { dependsOn(makeFile) }
            tasks.withType(com.android.build.gradle.tasks.MergeResources::class.java)
              .configureEach { dependsOn(makeFile) }
            tasks.withType(com.android.build.gradle.tasks.ManifestProcessorTask::class.java)
              .configureEach { dependsOn(makeFile)}

          }
            """
          )
        }
      )
    }

    ProjectSpec("project") {
      addSubproject(appProject)
      addSubproject(androidSub1)
      addSettingsSpec(projectSettings.build())
      addBuildSpec(projectBuild.build())
    }
      .writeIn(testProjectDir.toPath())

    shouldSucceed("moduleCheck")

    // one last check to make sure the manifest wasn't generated, since that would invalidate the test
    File(testProjectDir, "/lib1/src/main/AndroidManifest.xml").exists() shouldBe false
  }

  @Test
  fun `android test fixtures from android DSL should be treated as test fixtures`() {

    val androidSub1 = ProjectSpec("lib-1") {
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/testFixtures/java/com/modulecheck/lib1")) {
          addKotlinFile(
            "TestUtils.kt",
            """
            package com.modulecheck.lib1

            class TestUtils
            """.trimIndent()
          )
        }
      )
      addBuildSpec(
        ProjectBuildSpec {
          addPlugin("""id("com.android.library")""")
          addPlugin("kotlin(\"android\")")
          android = true
          addBlock(
            """
            android {
              testFixtures.enable = true
            }
            """.trimIndent()
          )
        }
      )
    }

    val androidSub2 = ProjectSpec("lib-2") {
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/test/java/com/modulecheck/lib2")) {
          addKotlinFile(
            "SomeTest.kt",
            """
            package com.modulecheck.lib2

            import com.modulecheck.lib1.TestUtils

            class SomeTest {

              val utils = TestUtils()
            }
            """.trimIndent()
          )
        }
      )
      addBuildSpec(
        ProjectBuildSpec {
          addPlugin("""id("com.android.library")""")
          addPlugin("kotlin(\"android\")")
          android = true
          addRawDependency("testImplementation(testFixtures(project(\":lib-1\")))")
        }
      )
    }

    ProjectSpec("project") {
      addSubproject(androidSub1)
      addSubproject(androidSub2)
      addSettingsSpec(projectSettings.build())
      addBuildSpec(projectBuild.build())
    }
      .writeIn(testProjectDir.toPath())

    shouldSucceed("moduleCheck")
  }
}
