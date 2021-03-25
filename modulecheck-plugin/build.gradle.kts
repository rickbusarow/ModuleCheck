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
  javaLibrary
  id("com.gradle.plugin-publish") version "0.13.0"
  id("java-gradle-plugin")
  `kotlin-dsl`
  `maven-publish`
}

dependencies {
  compileOnly(gradleApi())

  implementation(libs.anvil)
  implementation(libs.androidGradlePlugin)
  implementation(libs.kotlinCompiler)
  implementation(libs.kotlinGradlePlugin)
  implementation(libs.kotlinReflect)
  implementation(libs.kotlinPoet)
  implementation(libs.semVer)
  implementation(libs.javaParser)

  implementation(projects.modulecheckApi)
  implementation(projects.modulecheckCore)
  implementation(projects.modulecheckPsi)

  testImplementation(libs.bundles.jUnit)
  testImplementation(libs.bundles.kotest)
  testImplementation(libs.bundles.hermit)

  testImplementation(projects.modulecheckInternalTesting)
  testImplementation(projects.modulecheckSpecs)
}

gradlePlugin {
  plugins {
    create("moduleCheck") {
      id = PluginCoordinates.ID
      group = PluginCoordinates.GROUP
      implementationClass = PluginCoordinates.IMPLEMENTATION_CLASS
      version = "0.10.0"
    }
  }
}

// Configuration Block for the Plugin Marker artifact on Plugin Central
pluginBundle {
  website = PluginBundle.WEBSITE
  vcsUrl = PluginBundle.VCS
  description = PluginBundle.DESCRIPTION
  tags = PluginBundle.TAGS

  plugins {
    getByName("moduleCheck") {
      displayName = PluginBundle.DISPLAY_NAME
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

object PluginCoordinates {
  const val ID = "com.rickbusarow.module-check"
  const val GROUP = "com.rickbusarow.modulecheck"
  const val IMPLEMENTATION_CLASS = "modulecheck.gradle.ModuleCheckPlugin"
}

object PluginBundle {
  const val VCS = "https://github.com/RBusarow/ModuleCheck"
  const val WEBSITE = "https://github.com/RBusarow/ModuleCheck"
  const val DESCRIPTION = "Fast dependency graph validation for gradle"
  const val DISPLAY_NAME = "Fast dependency graph validation for gradle"
  val TAGS = listOf("plugin", "gradle")
}
