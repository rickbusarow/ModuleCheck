/*
 * Copyright (C) 2021-2023 Rick Busarow
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

package modulecheck.builds

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

abstract class KotlinJvmConventionPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    target.plugins.applyOnce("org.jetbrains.kotlin.jvm")

    target.extensions.configure(KotlinJvmProjectExtension::class.java) { extension ->
      extension.jvmToolchain { toolChain ->
        toolChain.languageVersion.set(JavaLanguageVersion.of(target.JDK))
      }
    }
    target.tasks.withType(JavaCompile::class.java) { task ->
      task.options.release.set(target.JVM_TARGET_INT)
      task.targetCompatibility = target.JVM_TARGET
    }
    target.extensions.configure(JavaPluginExtension::class.java) { extension ->
      extension.sourceCompatibility = JavaVersion.toVersion(target.JVM_TARGET)
    }
    target.tasks.withType(KotlinCompile::class.java) { task ->
      task.kotlinOptions {
        allWarningsAsErrors = false

        val kotlinMajor = target.KOTLIN_API
        languageVersion = kotlinMajor
        apiVersion = kotlinMajor

        jvmTarget = target.JVM_TARGET

        freeCompilerArgs += buildList {
          add("-Xinline-classes")
          add("-Xjvm-default=all")
          add("-Xsam-conversions=class")
          add("-opt-in=kotlin.ExperimentalStdlibApi")
          add("-opt-in=kotlin.RequiresOptIn")
          add("-opt-in=kotlin.contracts.ExperimentalContracts")
          add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
          add("-opt-in=kotlinx.coroutines.FlowPreview")

          // Workaround for Kotest's shading of the `@Language` annotation
          // https://github.com/kotest/kotest/issues/3387
          // It's fixed by https://github.com/kotest/kotest/pull/3397 but that's unreleased, so check
          // back after the next update.
          if (task.name == "compileTestKotlin" ||
            target.path == ":modulecheck-internal-testing" ||
            target.path == ":modulecheck-project-generation:api"
          ) {
            val kotestVersion = target.libsCatalog.version("kotest")
            check(kotestVersion == "5.5.5") {
              "The Kotest `@Language` workaround should be fixed in version $kotestVersion.  " +
                "Check to see if the opt-in compiler argument can be removed."
            }
            add("-opt-in=io.kotest.common.KotestInternal")
          }
        }
      }
    }

    target.tasks.register("lintMain") { task ->
      task.doFirst {
        target.tasks.withType(KotlinCompile::class.java) { compileTask ->
          compileTask.kotlinOptions {
            allWarningsAsErrors = true
          }
        }
      }
      task.finalizedBy(target.tasks.withType(KotlinCompile::class.java))
    }

    target.tasks.register("testJvm") { it.dependsOn("test") }
    target.tasks.register("buildTests") { it.dependsOn("testClasses") }
    target.tasks.register("moveJavaSrcToKotlin") { task ->

      task.doLast {
        val reg = """.*/src/([^/]*)/java.*""".toRegex()

        target.projectDir.walkTopDown()
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

        target.projectDir.walkBottomUp()
          .filter { it.path.matches(reg) }
          .forEach { file -> file.deleteRecursively() }
      }
    }
  }
}
