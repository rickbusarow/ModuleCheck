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

plugins {
  kotlin("jvm")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>()
  .configureEach {
    kotlinOptions {
      allWarningsAsErrors = false

      val kotlinMajor = "1.6"

      languageVersion = kotlinMajor
      apiVersion = kotlinMajor

      val javaMajor = "11"

      jvmTarget = javaMajor
      sourceCompatibility = javaMajor
      targetCompatibility = javaMajor

      freeCompilerArgs = freeCompilerArgs + listOf(
        "-Xinline-classes",
        "-Xjvm-default=enable",
        "-Xsam-conversions=class",
        "-opt-in=kotlin.ExperimentalStdlibApi",
        "-opt-in=kotlin.RequiresOptIn",
        "-opt-in=kotlin.contracts.ExperimentalContracts",
        "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        "-opt-in=kotlinx.coroutines.FlowPreview"
      )
    }
  }

tasks.register("moveJavaSrcToKotlin") {
  doLast {
    val reg = """.*/src/([^/]*)/java.*""".toRegex()

    projectDir.walkTopDown()
      .filter { it.path.matches(reg) }
      .forEach { file ->

        val oldPath = file.path
        val newPath = oldPath.replace("/java", "/kotlin")

        if (file.isFile) {
          val text = file.readText()

          File(newPath).also {
            it.createNewFile()
            it.writeText(text)
          }
        } else {

          File(newPath).mkdirs()
        }
      }

    projectDir.walkBottomUp()
      .filter { it.path.matches(reg) }
      .forEach { file ->

        file.deleteRecursively()
      }
  }
}
