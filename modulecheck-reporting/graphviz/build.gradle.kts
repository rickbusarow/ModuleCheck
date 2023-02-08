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

plugins {
  id("mcbuild")
}

mcbuild {
  artifactId = "modulecheck-reporting-graphviz"
  anvil()
}

dependencies {

  api(project(path = ":modulecheck-api"))
  api(project(path = ":modulecheck-config:api"))
  api(project(path = ":modulecheck-model:dependency:api"))
  api(project(path = ":modulecheck-model:dependency:api"))

  implementation(libs.graphviz.java.min)
  implementation(libs.rickBusarow.dispatch.core)

  implementation(project(path = ":modulecheck-model:dependency:api"))
  implementation(project(path = ":modulecheck-project:api"))
  implementation(project(path = ":modulecheck-utils:stdlib"))

  testImplementation(libs.bundles.hermit)
  testImplementation(libs.bundles.jUnit)
  testImplementation(libs.bundles.kotest)
  testImplementation(libs.kotlin.compiler)
  testImplementation(libs.kotlin.reflect)
  testImplementation(libs.rickBusarow.dispatch.test.core)

  testImplementation(project(path = ":modulecheck-model:sourceset:api"))
  testImplementation(project(path = ":modulecheck-project-generation:api"))
  testImplementation(project(path = ":modulecheck-project:api"))
  testImplementation(project(path = ":modulecheck-rule:impl"))
  testImplementation(project(path = ":modulecheck-runtime:testing"))
}
