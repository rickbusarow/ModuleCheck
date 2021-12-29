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

plugins {
  id("com.rickbusarow.gradle-dependency-sync") version "0.11.4"
  base
}

dependencies {

  dependencySync("app.cash.turbine:turbine:0.7.0")

  dependencySync("com.android.tools.build:gradle:7.0.3")
  dependencySync("com.github.ben-manes:gradle-versions-plugin:0.39.0")
  dependencySync("com.github.javaparser:javaparser-symbol-solver-core:3.23.1")
  dependencySync("com.github.tschuchortdev:kotlin-compile-testing:1.4.7")
  dependencySync("com.google.dagger:dagger-compiler:2.40.5")
  dependencySync("com.google.dagger:dagger:2.40.5")
  dependencySync("com.rickbusarow.dispatch:dispatch-android-espresso:1.0.0-beta10")
  dependencySync("com.rickbusarow.dispatch:dispatch-android-lifecycle-extensions:1.0.0-beta10")
  dependencySync("com.rickbusarow.dispatch:dispatch-android-lifecycle:1.0.0-beta10")
  dependencySync("com.rickbusarow.dispatch:dispatch-android-viewmodel:1.0.0-beta10")
  dependencySync("com.rickbusarow.dispatch:dispatch-core:1.0.0-beta10")
  dependencySync("com.rickbusarow.dispatch:dispatch-detekt:1.0.0-beta10")
  dependencySync("com.rickbusarow.dispatch:dispatch-test-junit4:1.0.0-beta10")
  dependencySync("com.rickbusarow.dispatch:dispatch-test-junit5:1.0.0-beta10")
  dependencySync("com.rickbusarow.dispatch:dispatch-test:1.0.0-beta10")
  dependencySync("com.rickbusarow.hermit:hermit-core:0.9.5")
  dependencySync("com.rickbusarow.hermit:hermit-coroutines:0.9.5")
  dependencySync("com.rickbusarow.hermit:hermit-junit4:0.9.5")
  dependencySync("com.rickbusarow.hermit:hermit-junit5:0.9.5")
  dependencySync("com.rickbusarow.hermit:hermit-mockk:0.9.5")
  dependencySync("com.squareup.anvil:annotations:2.3.10")
  dependencySync("com.squareup.anvil:compiler-api:2.3.10")
  dependencySync("com.squareup.anvil:compiler-utils:2.3.10")
  dependencySync("com.squareup.anvil:compiler:2.3.10")
  dependencySync("com.squareup.anvil:gradle-plugin:2.3.10")
  dependencySync("com.squareup:kotlinpoet:1.10.2")
  dependencySync("com.vanniktech:gradle-maven-publish-plugin-base:0.18.0")
  dependencySync("com.vanniktech:gradle-maven-publish-plugin:0.18.0")

  dependencySync("gradle.plugin.dev.arunkumar:scabbard-gradle-plugin:0.5.0")

  dependencySync("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.19.0")
  dependencySync("io.kotest:kotest-assertions-core-jvm:5.0.3")
  dependencySync("io.kotest:kotest-property-jvm:5.0.3")
  dependencySync("io.kotest:kotest-runner-junit5-jvm:5.0.3")

  dependencySync("javax.inject:javax.inject:1")

  dependencySync("net.swiftzer.semver:semver:1.2.0")

  dependencySync("org.antlr:antlr4-runtime:4.9.3")
  dependencySync("org.antlr:antlr4:4.9.3")
  dependencySync("org.codehaus.groovy:groovy-xml:3.0.9")
  dependencySync("org.codehaus.groovy:groovy:3.0.9")
  dependencySync("org.jetbrains.dokka:dokka-gradle-plugin:1.6.10")
  dependencySync("org.jetbrains.kotlin:kotlin-annotation-processing-embeddable:1.6.10")
  dependencySync("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.6.10")
  dependencySync("org.jetbrains.kotlin:kotlin-gradle-plugin-api:1.6.10")
  dependencySync("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10")
  dependencySync("org.jetbrains.kotlin:kotlin-reflect:1.6.10")
  dependencySync("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.10")
  dependencySync("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")
  dependencySync("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:1.6.0")
  dependencySync("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.0")
  dependencySync("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
  dependencySync("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.0")
  dependencySync("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.0")
  dependencySync("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.0")
  dependencySync("org.jetbrains.kotlinx:kotlinx-knit:0.3.0")
  dependencySync("org.jlleitschuh.gradle:ktlint-gradle:10.2.1")
  dependencySync("org.junit.jupiter:junit-jupiter-api:5.8.2")
  dependencySync("org.junit.jupiter:junit-jupiter-engine:5.8.2")
  dependencySync("org.junit.jupiter:junit-jupiter-params:5.8.2")
  dependencySync("org.unbescape:unbescape:1.1.6.RELEASE")
}
