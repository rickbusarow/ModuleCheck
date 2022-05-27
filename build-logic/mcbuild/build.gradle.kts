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

// `alias(libs.______)` inside the plugins block throws a false positive warning
// https://youtrack.jetbrains.com/issue/KTIJ-19369
// There's also an IntelliJ plugin to disable this warning globally:
// https://plugins.jetbrains.com/plugin/18949-gradle-libs-error-suppressor
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  `kotlin-dsl`
  base
  alias(libs.plugins.ktlint)
  alias(libs.plugins.google.ksp)
}

val kotlinVersion = libs.versions.kotlin.get()

dependencies {

  compileOnly(gradleApi())
  compileOnly(libs.kotlin.gradle.plug)

  implementation(libs.benManes.versions)
  implementation(libs.detekt.gradle)
  implementation(libs.dokka.gradle)
  implementation(libs.dropbox.dependencyGuard)
  implementation(libs.google.dagger.api)
  implementation(libs.google.ksp)
  implementation(libs.kotlin.compiler)
  implementation(libs.kotlinx.knit)
  implementation(libs.ktlint.gradle)
  implementation(libs.scabbard)
  implementation(libs.square.anvil.gradle)
  implementation(libs.square.moshi)
  implementation(libs.vanniktech.publish)

  ksp(libs.square.moshi.codegen)
}

java {
  // This is different from the Kotlin jvm target.
  toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions {

    languageVersion = "1.5"
    apiVersion = "1.5"

    jvmTarget = "11"

    freeCompilerArgs = freeCompilerArgs + listOf(
      "-opt-in=kotlin.RequiresOptIn"
    )
  }
}
