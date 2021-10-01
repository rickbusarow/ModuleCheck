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

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.detekt

buildscript {
  repositories {
    mavenCentral()
    google()
    maven("https://plugins.gradle.org/m2/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
  }
  dependencies {
    classpath("com.android.tools.build:gradle:7.0.2")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.31")
    classpath("org.jetbrains.kotlinx:kotlinx-knit:0.3.0")
    classpath("com.vanniktech:gradle-maven-publish-plugin:0.18.0")
    classpath("org.jmailen.gradle:kotlinter-gradle:3.6.0")
  }
}

plugins {
  id("com.github.ben-manes.versions") version "0.39.0"
  id("io.gitlab.arturbosch.detekt") version "1.18.1"
  id("org.jetbrains.dokka") version "1.5.30"
  id("com.osacky.doctor") version "0.7.3"
  id("com.dorongold.task-tree") version "2.1.0"
  base
  website
}

allprojects {

  repositories {
    google()
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://jitpack.io")
    maven("https://s3-us-west-2.amazonaws.com/si-mobile-sdks/android/")
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
  exclude("**/resources/**", "**/build/**", "**/src/test/java**", "**/src/test/kotlin**")

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

allprojects {
  apply(plugin = "org.jmailen.kotlinter")

  extensions.configure<org.jmailen.gradle.kotlinter.KotlinterExtension> {

    ignoreFailures = false
    reporters = arrayOf("checkstyle", "plain")
    experimentalRules = true
    disabledRules = arrayOf(
      "no-multi-spaces",
      "no-wildcard-imports",
      "max-line-length", // manually formatting still does this, and KTLint will still wrap long chains when possible
      "filename", // same as Detekt's MatchingDeclarationName, except Detekt's version can be suppressed and this can't
      "experimental:argument-list-wrapping" // doesn't work half the time
    )
  }
}

val kotlinVersion = libs.versions.kotlin.get()

allprojects {
  configurations.all {
    resolutionStrategy {
      force("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
      eachDependency {
        when {
          requested.group == "org.jetbrains.kotlin" -> useVersion(kotlinVersion)
        }
      }
    }
  }
}

// Delete any empty directories while cleaning.
allprojects {
  val proj = this@allprojects

  proj.tasks
    .withType<Delete>()
    .configureEach {
      doLast {

        val subprojectDirs = proj.subprojects
          .map { it.projectDir.path }

        proj.projectDir.walkBottomUp()
          .filter { it.isDirectory }
          .filterNot { dir -> subprojectDirs.any { dir.path.startsWith(it) } }
          .filterNot { it.path.contains(".gradle") }
          .filter { it.listFiles()?.isEmpty() != false }
          .forEach { it.deleteRecursively() }
      }
    }
}
