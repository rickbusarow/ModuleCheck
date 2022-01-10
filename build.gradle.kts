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

buildscript {
  dependencies {
    classpath(libs.agp)
    classpath(libs.kotlin.gradle.plug)
    classpath(libs.ktlint.gradle)
    classpath(libs.vanniktech.publish)
  }
}

@Suppress("UnstableApiUsage")
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
