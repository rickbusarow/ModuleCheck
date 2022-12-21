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

package modulecheck.builds

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

abstract class JavaLibraryConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {

    target.plugins.applyOnce("org.jetbrains.kotlin.jvm")

    target.extensions.configure(JavaPluginExtension::class.java) { extension ->
      extension.toolchain.languageVersion.set(JavaLanguageVersion.of(11))
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
  }
}
