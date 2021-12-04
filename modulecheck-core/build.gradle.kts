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
}

mcbuild {
  artifactId = "modulecheck-core"
  dagger = true
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
  api(project(path = ":modulecheck-parsing:core"))
  api(project(path = ":modulecheck-parsing:groovy-antlr"))
  api(project(path = ":modulecheck-parsing:java"))
  api(project(path = ":modulecheck-parsing:psi"))
  api(project(path = ":modulecheck-parsing:xml"))
  api(project(path = ":modulecheck-reporting:checkstyle"))
  api(project(path = ":modulecheck-reporting:console"))
  api(project(path = ":modulecheck-reporting:graphviz"))
  api(project(path = ":modulecheck-runtime"))
  api(project(path = ":modulecheck-utils"))

  implementation(libs.agp)
  implementation(libs.groovy)
  implementation(libs.groovyXml)
  implementation(libs.semVer)

  testImplementation(libs.bundles.hermit)
  testImplementation(libs.bundles.jUnit)
  testImplementation(libs.bundles.kotest)
  testImplementation(libs.kotlin.reflect)
  testImplementation(libs.rickBusarow.dispatch.test.core)

  testImplementation(project(path = ":modulecheck-internal-testing"))
  testImplementation(project(path = ":modulecheck-specs"))

  testImplementation(testFixtures(project(path = ":modulecheck-api")))
  testImplementation(testFixtures(project(path = ":modulecheck-project:api")))
  testImplementation(testFixtures(project(path = ":modulecheck-runtime")))
}
