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
import io.kotest.matchers.shouldBe
import modulecheck.specs.ProjectBuildSpec
import modulecheck.specs.ProjectSettingsSpec
import modulecheck.specs.ProjectSpec
import modulecheck.specs.ProjectSrcSpec
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

class UnusedKaptTest : BaseTest() {

  fun File.relativePath() = path.removePrefix(testProjectDir.path)

  @Test
  fun `unused`() {
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
        ProjectSpec("app") {
          addBuildSpec(
            ProjectBuildSpec {
              addPlugin("kotlin(\"jvm\")")
              addPlugin("kotlin(\"kapt\")")
              addExternalDependency("kapt", "com.google.dagger:dagger-compiler:2.30.1")
              addExternalDependency("kaptTest", "com.squareup.moshi:moshi-kotlin-codegen:1.11.0")
            }
          )
        }
      )
    }
      .writeIn(testProjectDir.toPath())

    val result = gradleRunner
      .withArguments("moduleCheckKapt")
      .build()

    println(result.output)

    result.task(":moduleCheckKapt")?.outcome shouldBe TaskOutcome.SUCCESS
  }

  @Test
  fun `used`() {
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
        ProjectSpec("app") {
          addSrcSpec(
            ProjectSrcSpec(Path.of("src/main/kotlin")) {
              addFile(
                FileSpec.builder("com.my.app", "App.kt")
                  .addType(
                    TypeSpec.classBuilder("MyClass")
                      .addFunction(
                        FunSpec.constructorBuilder()
                          .addAnnotation(ClassName("javax.inject", "Inject"))
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
              addPlugin("kotlin(\"kapt\")")
              addExternalDependency("kapt", "com.google.dagger:dagger-compiler:2.30.1")
              addExternalDependency("kaptTest", "com.squareup.moshi:moshi-kotlin-codegen:1.11.0")
            }
          )
        }
      )
    }
      .writeIn(testProjectDir.toPath())

    val result = gradleRunner
      .withArguments("moduleCheckKapt")
      .build()

    println(result.output)

    result.task(":moduleCheckKapt")?.outcome shouldBe TaskOutcome.SUCCESS
  }
}
