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
    classpath(libs.kotlin.serialization)
  }
}

plugins {
  base
  kotlin("jvm")
  alias(libs.plugins.google.ksp)
  id("java-gradle-plugin")
}

gradlePlugin {
  plugins {
    create("artifacts-check") {
      id = "mcbuild.artifacts-check"
      implementationClass = "modulecheck.builds.artifacts.ArtifactsPlugin"
    }
  }
}

dependencies {

  api(libs.square.moshi)

  compileOnly(gradleApi())

  compileOnly(libs.kotlin.gradle.plugin)

  implementation(libs.benManes.versions)
  implementation(libs.detekt.gradle)
  implementation(libs.dokka.gradle)
  implementation(libs.dropbox.dependencyGuard)
  implementation(libs.google.dagger.api)
  implementation(libs.google.ksp)
  implementation(libs.kotlin.compiler)
  implementation(libs.kotlin.reflect)
  implementation(libs.kotlin.stdlib.common)
  implementation(libs.kotlin.stdlib.jdk7)
  implementation(libs.kotlin.stdlib.jdk8)
  implementation(libs.kotlinx.knit)

  implementation(project(path = ":core"))

  ksp(libs.square.moshi.codegen)
}
