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
    create("ben-manes") {
      id = "mcbuild.ben-manes"
      implementationClass = "modulecheck.builds.BenManesVersionsPlugin"
    }
    create("mcbuild.clean") {
      id = "mcbuild.clean"
      implementationClass = "modulecheck.builds.CleanPlugin"
    }
    create("mcbuild.dependency-guard") {
      id = "mcbuild.dependency-guard"
      implementationClass = "modulecheck.builds.DependencyGuardConventionPlugin"
    }
    create("mcbuild.dependency-guard-aggregate") {
      id = "mcbuild.dependency-guard-aggregate"
      implementationClass = "modulecheck.builds.DependencyGuardAggregatePlugin"
    }
    create("mcbuild.detekt") {
      id = "mcbuild.detekt"
      implementationClass = "modulecheck.builds.DetektConventionPlugin"
    }
    create("mcbuild.dokka") {
      id = "mcbuild.dokka"
      implementationClass = "modulecheck.builds.DokkaConventionPlugin"
    }
    create("mcbuild.dokka-version-archive") {
      id = "mcbuild.dokka-version-archive"
      implementationClass = "modulecheck.builds.DokkaVersionArchivePlugin"
    }
    create("mcbuild.kotlin") {
      id = "mcbuild.kotlin"
      implementationClass = "modulecheck.builds.KotlinJvmConventionPlugin"
    }
    create("mcbuild.knit") {
      id = "mcbuild.knit"
      implementationClass = "modulecheck.builds.KnitConventionPlugin"
    }
    create("mcbuild.ktlint") {
      id = "mcbuild.ktlint"
      implementationClass = "modulecheck.builds.KtLintConventionPlugin"
    }
    create("mcbuild.test") {
      id = "mcbuild.test"
      implementationClass = "modulecheck.builds.TestConventionPlugin"
    }
    create("mcbuild.website") {
      id = "mcbuild.website"
      implementationClass = "modulecheck.builds.WebsitePlugin"
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
  implementation(libs.dokka.versioning)
  implementation(libs.dropbox.dependencyGuard)
  implementation(libs.google.dagger.api)
  implementation(libs.google.ksp)
  implementation(libs.gradle.plugin.publish)
  implementation(libs.kotlin.compiler)
  implementation(libs.kotlin.gradle.plugin)
  implementation(libs.kotlin.reflect)
  implementation(libs.kotlin.stdlib.common)
  implementation(libs.kotlin.stdlib.jdk7)
  implementation(libs.kotlin.stdlib.jdk8)
  implementation(libs.kotlinx.binaryCompatibility)
  implementation(libs.kotlinx.knit)
  implementation(libs.rickBusarow.ktlint)
  implementation(libs.rickBusarow.ktrules)
  implementation(libs.scabbard)
  implementation(libs.square.anvil.gradle)
  implementation(libs.square.kotlinPoet)
  implementation(libs.vanniktech.publish)

  ksp(libs.square.moshi.codegen)
}
