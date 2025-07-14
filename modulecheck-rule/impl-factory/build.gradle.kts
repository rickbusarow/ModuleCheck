/*
 * Copyright (C) 2021-2025 Rick Busarow
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
  alias(libs.plugins.mahout.kotlin.jvm.module)
  alias(libs.plugins.anvil)
}

mahout {
  publishing {
    publishMaven(artifactId = "modulecheck-rule-impl-factory")
  }
}

dependencies {

  api(libs.kotlinx.coroutines.core)
  api(libs.kotlinx.coroutines.jvm)

  api(project(path = ":modulecheck-dagger"))
  api(project(path = ":modulecheck-finding:api"))
  api(project(path = ":modulecheck-project:api"))
  api(project(path = ":modulecheck-rule:api"))
  api(project(path = ":modulecheck-utils:trace"))
  api(project(path = ":modulecheck-utils:trace"))
  api(project(path = ":modulecheck-utils:trace"))

  implementation(project(path = ":modulecheck-api"))
  implementation(project(path = ":modulecheck-finding:impl"))
  implementation(project(path = ":modulecheck-model:dependency:api"))
  implementation(project(path = ":modulecheck-model:dependency:api"))
  implementation(project(path = ":modulecheck-utils:coroutines:api"))
  implementation(project(path = ":modulecheck-utils:stdlib"))
  implementation(project(path = ":modulecheck-utils:trace"))
  implementation(project(path = ":modulecheck-utils:trace"))

  testImplementation(libs.bundles.junit)
  testImplementation(libs.bundles.kotest)
}
