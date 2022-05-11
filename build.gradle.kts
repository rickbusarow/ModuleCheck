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

import modulecheck.gradle.moduleCheck

buildscript {
  dependencies {
    classpath(libs.agp)
    classpath(libs.kotlin.gradle.plug)
    classpath(libs.ktlint.gradle)
    classpath(libs.vanniktech.publish)
  }
}

// `alias(libs.______)` inside the plugins block throws a false positive warning
// https://youtrack.jetbrains.com/issue/KTIJ-19369
// There's also an IntelliJ plugin to disable this warning globally:
// https://plugins.jetbrains.com/plugin/18949-gradle-libs-error-suppressor
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  alias(libs.plugins.dependencyAnalysis)
  alias(libs.plugins.detekt)
  alias(libs.plugins.gradleDoctor)
  alias(libs.plugins.taskTree)
  id("mcbuild.ben-manes")
  id("mcbuild.clean")
  id("mcbuild.dokka")
  id("mcbuild.kotlin")
  id("mcbuild.ktlint")
  id("mcbuild.test")
  id("mcbuild.website")
  id("com.rickbusarow.module-check") version "0.12.0"
}

moduleCheck {
  deleteUnused = true
  checks {
    sortDependencies = true
  }
}

tasks.named("ktlintFormat") {
  dependsOn(gradle.includedBuild("build-logic").task(":ktlintFormat"))
}
tasks.named("ktlintCheck") {
  dependsOn(gradle.includedBuild("build-logic").task(":ktlintCheck"))
}
tasks.withType<Delete> {
  dependsOn(gradle.includedBuild("build-logic").task(":clean"))
}
doctor {
  disallowCleanTaskDependencies.set(false)
}
