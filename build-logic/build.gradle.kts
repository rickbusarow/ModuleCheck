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

buildscript {
  dependencies {
    // Gradle 7.6 has a dependency resolution bug which tries to use Kotlin 1.7.10
    // for transitive dependencies like `sam-with-receiver`.
    // https://github.com/gradle/gradle/issues/22510
    classpath(libs.kotlin.sam.with.receiver)
  }
}

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlinter)
  alias(libs.plugins.dependencyAnalysis)
  alias(libs.plugins.moduleCheck)
}

moduleCheck {
  deleteUnused = true
  checks {
    sortDependencies = true
  }
}

val kotlinVersion = libs.versions.kotlin.get()
allprojects {

  configurations.all {
    resolutionStrategy {
      eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
          useVersion(kotlinVersion)
        }
      }
    }
  }

  plugins.withType(JavaPlugin::class.java) {
    configure<JavaPluginExtension> {
      @Suppress("MagicNumber")
      toolchain.languageVersion.set(JavaLanguageVersion.of(11))
    }
  }

  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {

      languageVersion = "1.6"
      apiVersion = "1.6"

      jvmTarget = "11"

      freeCompilerArgs = freeCompilerArgs + listOf(
        "-opt-in=kotlin.RequiresOptIn"
      )
    }
  }
}

tasks.withType<Delete> rootTask@{

  subprojects {
    tasks.withType<Delete> subTask@{
      this@rootTask.dependsOn(this@subTask)
    }
  }
}

tasks.named("test") rootTask@{

  subprojects {
    tasks.withType<Test> subTask@{
      useJUnitPlatform()
      this@rootTask.dependsOn(this@subTask)
    }
  }
}
