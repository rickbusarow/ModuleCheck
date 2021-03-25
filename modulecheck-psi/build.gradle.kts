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

  compileOnly("org.codehaus.groovy:groovy-xml:3.0.7")

  compileOnly(gradleApi())

  implementation(libs.androidGradlePlugin)
  implementation(libs.kotlinCompiler)
  implementation(libs.kotlinGradlePlugin)
  implementation(libs.kotlinReflect)
  implementation(libs.kotlinPoet)
  implementation(libs.semVer)
  implementation(libs.javaParser)

  testImplementation(libs.bundles.jUnit)
  testImplementation(libs.bundles.kotest)
  testImplementation(libs.bundles.hermit)

  testImplementation(projects.modulecheckInternalTesting)
  testImplementation(projects.modulecheckSpecs)
}

publishing {
  publications {
    create<MavenPublication>("maven") {

      groupId = "com.rickbusarow.modulecheck"
      artifactId = "modulecheck-psi"

      version = libs.versions.versionName.get()

      from(components["java"])
    }
  }
}
