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
  id("com.gradle.plugin-publish") version "0.12.0"
  id("java-gradle-plugin")
  `kotlin-dsl`
  `maven-publish`
}

kotlinDslPluginOptions {
  experimentalWarning.set(false)
}

dependencies {
  compileOnly(gradleApi())

  implementation(BuildPlugins.anvil)
  implementation(BuildPlugins.androidGradlePlugin)
  implementation(Libs.Kotlin.compiler)
  implementation(Libs.Kotlin.gradlePlugin)
  implementation(Libs.Kotlin.reflect)
  implementation(Libs.Square.KotlinPoet.core)
  implementation(Libs.Swiftzer.semVer)
  implementation(Libs.javaParser)

  implementation(project(path = ":modulecheck-api"))
  implementation(project(path = ":modulecheck-core"))
  implementation(project(path = ":modulecheck-psi"))

  testImplementation(Libs.JUnit.api)
  testImplementation(Libs.JUnit.engine)
  testImplementation(Libs.JUnit.params)
  testImplementation(Libs.Kotest.assertions)
  testImplementation(Libs.Kotest.properties)
  testImplementation(Libs.Kotest.runner)
  testImplementation(Libs.RickBusarow.Hermit.core)
  testImplementation(Libs.RickBusarow.Hermit.junit5)

  testImplementation(project(path = ":modulecheck-specs"))
}

gradlePlugin {
  plugins {
    create("moduleCheck") {
      id = PluginCoordinates.ID
      group = PluginCoordinates.GROUP
      implementationClass = PluginCoordinates.IMPLEMENTATION_CLASS
      version = Versions.versionName
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
