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
  `maven-publish`
}

dependencies {

  implementation(projects.modulecheckApi)
  implementation(projects.modulecheckPsi)

  implementation(Libs.Groovy.xml)

  compileOnly(gradleApi())

  implementation(BuildPlugins.androidGradlePlugin)
  implementation(Libs.Kotlin.compiler)
  implementation(Libs.Kotlin.gradlePlugin)
  implementation(Libs.Kotlin.reflect)
  implementation(Libs.Square.KotlinPoet.core)
  implementation(Libs.Swiftzer.semVer)
  implementation(Libs.javaParser)

  testImplementation(Libs.JUnit.api)
  testImplementation(Libs.JUnit.engine)
  testImplementation(Libs.MockK.core)
  testImplementation(Libs.JUnit.params)
  testImplementation(Libs.Kotest.assertions)
  testImplementation(Libs.Kotest.properties)
  testImplementation(Libs.Kotest.runner)
  testImplementation(Libs.RickBusarow.Hermit.core)
  testImplementation(Libs.RickBusarow.Hermit.junit5)

  testImplementation(projects.modulecheckInternalTesting)
  testImplementation(projects.modulecheckSpecs)
}

publishing {
  publications {
    create<MavenPublication>("maven") {

      groupId = "com.rickbusarow.modulecheck"
      artifactId = "modulecheck-core"

      version = Versions.versionName

      from(components["java"])
    }
  }
}
