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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `kotlin-dsl`
}

repositories {
  mavenCentral()
  google()
}

val kotlinVersion = "1.4.32"

dependencies {

  compileOnly(gradleApi())

  implementation(kotlin("gradle-plugin", version = kotlinVersion))
  implementation(kotlin("stdlib", version = kotlinVersion))
  implementation(kotlin("stdlib-common", version = kotlinVersion))
  implementation(kotlin("stdlib-jdk7", version = kotlinVersion))
  implementation(kotlin("stdlib-jdk8", version = kotlinVersion))
  implementation(kotlin("reflect", version = kotlinVersion))

  implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
}

configurations.all {
  resolutionStrategy {

    eachDependency {
      when {
        requested.name.startsWith("kotlin-stdlib") -> {
          useTarget(
            "${requested.group}:${requested.name.replace("jre", "jdk")}:${requested.version}"
          )
        }
        requested.group == "org.jetbrains.kotlin" -> useVersion(kotlinVersion)
      }
    }
  }
}

tasks.withType<KotlinCompile>()
  .configureEach {

    kotlinOptions {

      freeCompilerArgs = listOf(
        "-Xinline-classes",
        "-Xopt-in=kotlin.ExperimentalStdlibApi",
        "-Xuse-experimental=kotlin.contracts.ExperimentalContracts"
      )
    }
  }
