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
  id("mcbuild")
  id("java-test-fixtures")
}

mcbuild {
  artifactId = "modulecheck-api"
  anvil = true
}

val isIdeSync = System.getProperty("idea.sync.active", "false").toBoolean()

dependencies {

  api(libs.kotlin.compiler)
  api(libs.kotlinx.coroutines.core)
  api(libs.kotlinx.coroutines.jvm)
  api(libs.rickBusarow.dispatch.core)
  api(libs.semVer)

  api(project(path = ":modulecheck-dagger"))
  api(project(path = ":modulecheck-parsing:android"))
  api(project(path = ":modulecheck-parsing:gradle"))
  api(project(path = ":modulecheck-parsing:source"))
  api(project(path = ":modulecheck-project:api"))
  api(project(path = ":modulecheck-utils"))

  implementation(libs.agp)
  implementation(libs.groovy)
  implementation(libs.groovyXml)
  implementation(libs.kotlin.reflect)

  testFixturesApi(libs.bundles.hermit)

  if (isIdeSync) {
    compileOnly(project(path = ":modulecheck-internal-testing"))
    compileOnly(libs.bundles.hermit)
    compileOnly(libs.bundles.jUnit)
    compileOnly(libs.bundles.kotest)
  }

  testImplementation(libs.bundles.hermit)
  testImplementation(libs.bundles.jUnit)
  testImplementation(libs.bundles.kotest)
}
