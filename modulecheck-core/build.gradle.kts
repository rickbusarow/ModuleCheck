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

plugins {
  id("mcbuild")
}

mcbuild {
  artifactId = "modulecheck-core"
  anvil = true
}

dependencies {

  api(libs.kotlin.compiler)
  api(libs.kotlinx.coroutines.core)
  api(libs.kotlinx.coroutines.core)
  api(libs.kotlinx.coroutines.jvm)
  api(libs.kotlinx.coroutines.jvm)
  api(libs.rickBusarow.dispatch.core)
  api(libs.rickBusarow.dispatch.core)

  api(project(path = ":modulecheck-api"))
  api(project(path = ":modulecheck-config:api"))
  api(project(path = ":modulecheck-finding:api"))
  api(project(path = ":modulecheck-finding:name"))
  api(project(path = ":modulecheck-parsing:gradle:dsl:api"))
  api(project(path = ":modulecheck-parsing:gradle:model:api"))
  api(project(path = ":modulecheck-parsing:source:api"))
  api(project(path = ":modulecheck-project:api"))
  api(project(path = ":modulecheck-rule:api"))
  api(project(path = ":modulecheck-utils"))

  implementation(libs.groovy)
  implementation(libs.groovyXml)
  implementation(libs.semVer)

  testImplementation(libs.bundles.hermit)
  testImplementation(libs.bundles.jUnit)
  testImplementation(libs.bundles.kotest)
  testImplementation(libs.kotlin.reflect)
  testImplementation(libs.rickBusarow.dispatch.test.core)

  testImplementation(project(path = ":modulecheck-config:fake"))
  testImplementation(project(path = ":modulecheck-internal-testing"))
  testImplementation(project(path = ":modulecheck-parsing:source:api"))
  testImplementation(project(path = ":modulecheck-project:testing"))
  testImplementation(project(path = ":modulecheck-runtime:testing"))
  testImplementation(project(path = ":modulecheck-utils"))
}
