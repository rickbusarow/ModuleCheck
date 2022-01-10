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

@Suppress("UnstableApiUsage")
plugins {
  `kotlin-dsl`
  base
  id("org.jlleitschuh.gradle.ktlint")
}

val kotlinVersion = libs.versions.kotlin.get()

dependencies {

  compileOnly(gradleApi())

  implementation(libs.agp)
  implementation(libs.benManes.gradle)
  implementation(libs.detekt.gradle)
  implementation(libs.dokka.gradle)
  implementation(libs.google.dagger.api)
  implementation(libs.kotlin.compiler)
  implementation(libs.kotlin.gradle.plug)
  implementation(libs.ktlint.gradle)
  implementation(libs.scabbard)
  implementation(libs.square.anvil.gradle)
  implementation(libs.vanniktech.publish)
}

java {
  // force Java 8 source when building java-only artifacts.
  // This is different from the Kotlin jvm target.
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

extensions.configure(org.jlleitschuh.gradle.ktlint.KtlintExtension::class.java) {
  debug.set(false)

  outputToConsole.set(true)
  disabledRules.set(
    setOf(
      "no-wildcard-imports",
      "max-line-length", // manually formatting still does this, and KTLint will still wrap long chains when possible
      "filename", // same as Detekt's MatchingDeclarationName, but Detekt's version can be suppressed and this can't
      "experimental:argument-list-wrapping" // doesn't work half the time
    )
  )
}
