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

import modulecheck.builds.VERSION_NAME

plugins {
  id("mcbuild")
  id("com.gradle.plugin-publish") version "0.21.0"
  id("java-gradle-plugin")
  `maven-publish`
}

mcbuild {
  // artifactId = "com.rickbusarow.module-check"
  dagger = true
}

tasks.withType<Test> {
  // Gradle TestKit somewhat regularly runs out of memory on the freebie GitHub runners
  maxParallelForks = 1
}

dependencies {

  api(libs.javax.inject)
  api(libs.kotlin.compiler)
  api(libs.kotlinx.coroutines.core)
  api(libs.kotlinx.coroutines.jvm)
  api(libs.rickBusarow.dispatch.core)

  api(project(path = ":modulecheck-api"))
  api(project(path = ":modulecheck-core"))
  api(project(path = ":modulecheck-dagger"))
  api(project(path = ":modulecheck-parsing:android"))
  api(project(path = ":modulecheck-parsing:gradle"))
  api(project(path = ":modulecheck-parsing:source"))
  api(project(path = ":modulecheck-parsing:wiring"))
  api(project(path = ":modulecheck-project:api"))
  api(project(path = ":modulecheck-project:impl"))
  api(project(path = ":modulecheck-runtime"))

  compileOnly(gradleApi())

  implementation(libs.agp)
  implementation(libs.kotlin.gradle.plug)
  implementation(libs.kotlin.gradle.plugin.api)
  implementation(libs.kotlin.reflect)
  implementation(libs.semVer)
  implementation(libs.square.anvil.gradle)

  testImplementation(libs.bundles.hermit)
  testImplementation(libs.bundles.jUnit)
  testImplementation(libs.bundles.kotest)
  testImplementation(libs.kotlinPoet)

  testImplementation(project(path = ":modulecheck-specs"))
  testImplementation(project(path = ":modulecheck-utils"))

  testImplementation(testFixtures(project(path = ":modulecheck-api")))
  testImplementation(testFixtures(project(path = ":modulecheck-project:api")))
}

gradlePlugin {
  plugins {
    create("moduleCheck") {
      id = "com.rickbusarow.module-check"
      group = "com.rickbusarow.modulecheck"
      implementationClass = "modulecheck.gradle.ModuleCheckPlugin"
      version = VERSION_NAME
    }
  }
}

java {
  withSourcesJar()
  withJavadocJar()
}

// Configuration Block for the Plugin Marker artifact on Plugin Central
pluginBundle {
  website = "https://github.com/RBusarow/ModuleCheck"
  vcsUrl = "https://github.com/RBusarow/ModuleCheck"
  description = "Fast dependency graph validation for gradle"
  tags = listOf("kotlin", "dependencies", "android", "gradle-plugin", "kotlin-compiler-plugin")

  (plugins) {
    getByName("moduleCheck") {
      displayName = "Fast dependency graph validation for gradle"
    }
  }
}

tasks.create("setupPluginUploadFromEnvironment") {
  doLast {
    val key = System.getenv("GRADLE_PUBLISH_KEY")
    val secret = System.getenv("GRADLE_PUBLISH_SECRET")

    if (key == null || secret == null) {
      throw GradleException(
        "gradlePublishKey and/or gradlePublishSecret are not defined environment variables"
      )
    }

    System.setProperty("gradle.publish.key", key)
    System.setProperty("gradle.publish.secret", secret)
  }
}
