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

import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.dependencyAnalysis)
  alias(libs.plugins.moduleCheck)
  alias(libs.plugins.ktlint) apply false
}

moduleCheck {
  deleteUnused = true
  checks {
    sortDependencies = true
  }
}

val kotlinVersion = libs.versions.kotlin.get()
val ktlintPluginId = libs.plugins.ktlint.get().pluginId

allprojects ap@{

  val innerProject = this@ap

  apply(plugin = ktlintPluginId)
  dependencies {
    "ktlint"(rootProject.libs.rickBusarow.ktrules)
  }

  if (innerProject != rootProject) {
    rootProject.tasks.named("ktlintCheck") {
      dependsOn(innerProject.tasks.named("ktlintCheck"))
    }
    rootProject.tasks.named("ktlintFormat") {
      dependsOn(innerProject.tasks.named("ktlintFormat"))
    }
  }

  plugins.withType(KotlinBasePlugin::class.java).configureEach {
    extensions.configure(KotlinJvmProjectExtension::class.java) {
      jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get()))
      }
    }
  }
  plugins.withType(JavaPlugin::class.java).configureEach {
    extensions.configure(JavaPluginExtension::class.java) {
      sourceCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
    }
  }

  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {

      languageVersion = "1.6"
      apiVersion = "1.6"

      jvmTarget = libs.versions.jvmTarget.get()

      freeCompilerArgs = freeCompilerArgs + listOf(
        "-opt-in=kotlin.RequiresOptIn"
      )
    }
  }
}

tasks.withType<Delete>().configureEach rootTask@{

  subprojects {
    tasks.withType<Delete>().forEach { subTask ->
      this@rootTask.dependsOn(subTask)
    }
  }
}

tasks.named("test") rootTask@{

  subprojects {
    tasks.withType<Test>().forEach { subTask ->
      subTask.useJUnitPlatform()
      this@rootTask.dependsOn(subTask)
    }
  }
}
