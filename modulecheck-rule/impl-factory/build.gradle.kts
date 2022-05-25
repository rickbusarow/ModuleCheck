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
  artifactId = "modulecheck-rule-impl-factory"
  anvil = true
}

dependencies {

  api(libs.kotlinx.coroutines.core)
  api(libs.kotlinx.coroutines.jvm)

  api(project(path = ":modulecheck-config:api"))
  api(project(path = ":modulecheck-dagger"))
  api(project(path = ":modulecheck-finding:api"))
  api(project(path = ":modulecheck-finding:impl"))
  api(project(path = ":modulecheck-project:api"))
  api(project(path = ":modulecheck-rule:api"))
  api(project(path = ":modulecheck-utils:coroutines"))
  api(project(path = ":modulecheck-utils:stdlib"))

  implementation(project(path = ":modulecheck-api"))
  implementation(project(path = ":modulecheck-parsing:gradle:model:api"))

  testImplementation(libs.bundles.hermit)
  testImplementation(libs.bundles.jUnit)
  testImplementation(libs.bundles.kotest)
}
