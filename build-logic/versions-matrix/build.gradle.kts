/*
 * Copyright (C) 2021-2025 Rick Busarow
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
  base
  kotlin("jvm")
  alias(libs.plugins.google.ksp)
  id("java-gradle-plugin")
}

gradlePlugin {
  plugins {
    create("mcbuild.matrix-yaml") {
      id = "mcbuild.matrix-yaml"
      implementationClass = "modulecheck.builds.matrix.VersionsMatrixYamlPlugin"
    }
  }
}

dependencies {

  api(libs.rickBusarow.kgx)
  api(libs.square.moshi)

  api(project(path = ":core"))

  compileOnly(gradleApi())

  compileOnly(libs.kotlin.gradle.plugin)

  implementation(libs.benManes.versions)
  implementation(libs.buildconfig)
  implementation(libs.detekt.gradle)
  implementation(libs.dokka.gradle)
  implementation(libs.dropbox.dependencyGuard)
  implementation(libs.google.dagger.api)
  implementation(libs.google.ksp)
  implementation(libs.kotlin.compiler)
  implementation(libs.kotlin.gradle.plugin)
  implementation(libs.kotlin.reflect)
  implementation(libs.kotlin.stdlib.common)
  implementation(libs.kotlin.stdlib.jdk7)
  implementation(libs.kotlin.stdlib.jdk8)
  implementation(libs.kotlinx.knit)
  implementation(libs.rickBusarow.ktlint)
  implementation(libs.scabbard)
  implementation(libs.square.anvil.gradle)
  implementation(libs.square.kotlinPoet)
  implementation(libs.vanniktech.publish)
  implementation(libs.semVer)

  ksp(libs.square.moshi.codegen)
}
