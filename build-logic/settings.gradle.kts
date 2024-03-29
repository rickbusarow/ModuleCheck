/*
 * Copyright (C) 2021-2023 Rick Busarow
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

rootProject.name = "build-logic"

pluginManagement {

  val allowMavenLocal = providers
    .gradleProperty("moduleCheck.allow-maven-local")
    .orNull.toBoolean()

  repositories {
    if (allowMavenLocal) {
      logger.lifecycle("${rootProject.name} -- allowing mavenLocal for plugins")
      mavenLocal()
    }
    mavenCentral()
    google()
    maven("https://plugins.gradle.org/m2/")
  }
}

val allowMavenLocal = providers
  .gradleProperty("moduleCheck.allow-maven-local")
  .orNull.toBoolean()

dependencyResolutionManagement {
  @Suppress("UnstableApiUsage")
  repositories {
    if (allowMavenLocal) {
      logger.lifecycle("${rootProject.name} -- allowing mavenLocal for dependencies")
      mavenLocal()
    }
    mavenCentral()
    google()
    maven("https://plugins.gradle.org/m2/")
  }

  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}

include(
  ":artifacts-check",
  ":conventions",
  ":core",
  ":mcbuild",
  ":versions-matrix"
)
