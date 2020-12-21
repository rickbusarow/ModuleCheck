/*
 * Copyright (C) 2020 Rick Busarow
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

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.detekt

buildscript {
  repositories {
    mavenCentral()
    google()
    jcenter()
    maven("https://plugins.gradle.org/m2/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
  }
  dependencies {
    classpath("com.android.tools.build:gradle:4.1.0")
    classpath("com.google.firebase:firebase-crashlytics-gradle:2.3.0")
    classpath("com.google.gms:google-services:4.3.4")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.20")
    classpath("com.vanniktech:gradle-maven-publish-plugin:0.13.0")
    classpath("com.jaredsburrows:gradle-spoon-plugin:1.5.0")
    classpath("org.jetbrains.kotlinx:kotlinx-knit:0.2.2")
    classpath("com.squareup.anvil:gradle-plugin:2.0.6")
  }
}

plugins {
  id(Plugins.benManes) version Versions.benManes
  id(Plugins.gradleDoctor) version Versions.gradleDoctor
  id(Plugins.spotless) version Versions.spotless
  id(Plugins.detekt) version "1.15.0"
  kotlin("jvm")
  id(Plugins.dokka) version Versions.dokka
  id(Plugins.taskTree) version Versions.taskTree
  base
}

allprojects {

  repositories {
    google()
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://jitpack.io")
    jcenter()
    maven("https://s3-us-west-2.amazonaws.com/si-mobile-sdks/android/")
  }

  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>()
    .configureEach {

      kotlinOptions {

        jvmTarget = "1.8"

        freeCompilerArgs = listOf(
          "-Xinline-classes",
          "-Xopt-in=kotlin.ExperimentalStdlibApi",
          "-Xuse-experimental=kotlin.contracts.ExperimentalContracts"
        )
      }
    }
}

@Suppress("DEPRECATION")
detekt {

  parallel = true
  config = files("$rootDir/detekt/detekt-config.yml")

  reports {
    xml.enabled = false
    html.enabled = true
    txt.enabled = false
  }
}

tasks.withType<DetektCreateBaselineTask> {

  setSource(files(rootDir))

  include("**/*.kt", "**/*.kts")
  exclude("**/resources/**", "**/build/**", "**/src/test/java**")

  // Target version of the generated JVM bytecode. It is used for type resolution.
  this.jvmTarget = "1.8"
}

tasks.withType<Detekt> {

  setSource(files(rootDir))

  include("**/*.kt", "**/*.kts")
  exclude("**/resources/**", "**/build/**", "**/src/test/java**")

  // Target version of the generated JVM bytecode. It is used for type resolution.
  this.jvmTarget = "1.8"
}

fun isNonStable(version: String): Boolean {
  val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
  val regex = "^[0-9,.v-]+(-r)?$".toRegex()
  val isStable = stableKeyword || regex.matches(version)
  return isStable.not()
}

tasks.named(
  "dependencyUpdates",
  com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask::class.java
).configure {
  rejectVersionIf {
    isNonStable(candidate.version) && !isNonStable(currentVersion)
  }
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
  java {
    target("src/*/java/**/*.java")
    googleJavaFormat("1.7")
    removeUnusedImports()
    trimTrailingWhitespace()
    endWithNewline()
  }
  kotlin {
    target("**/src/**/*.kt", "**/src/**/*.kt")
    ktlint("0.40.0")
      .userData(
        mapOf(
          "indent_size" to "2",
          "continuation_indent_size" to "2",
          "max_line_length" to "off",
          "disabled_rules" to "no-wildcard-imports",
          "ij_kotlin_imports_layout" to "*,java.**,javax.**,kotlin.**,^"
        )
      )
    trimTrailingWhitespace()
    endWithNewline()
  }
  kotlinGradle {
    target("*.gradle.kts")
    ktlint("0.40.0")
      .userData(
        mapOf(
          "indent_size" to "2",
          "continuation_indent_size" to "2",
          "max_line_length" to "off",
          "disabled_rules" to "no-wildcard-imports",
          "ij_kotlin_imports_layout" to "*,java.**,javax.**,kotlin.**,^"
        )
      )
  }
}
