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
  artifactId = "modulecheck-project-testing"
  anvil()
}

dependencies {

  api(libs.bundles.hermit)
  api(libs.kotlinx.coroutines.debug)

  api(project(path = ":modulecheck-config:api"))
  api(project(path = ":modulecheck-internal-testing"))
  api(project(path = ":modulecheck-model:dependency:api"))
  api(project(path = ":modulecheck-parsing:kotlin-compiler:impl"))
  api(project(path = ":modulecheck-project-generation:api"))
  api(project(path = ":modulecheck-project:api"))

  compileOnly(gradleApi())

  implementation(libs.bundles.hermit)
  implementation(libs.bundles.jUnit)
  implementation(libs.bundles.kotest)
  implementation(libs.bundles.kotest)
  implementation(libs.kotlin.reflect)

  implementation(project(path = ":modulecheck-config:impl"))
  implementation(project(path = ":modulecheck-model:dependency:impl"))
}
