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
  published(artifactId = "modulecheck-parsing-element-api")
  anvil()
}

dependencies {
  api(libs.kotlin.compiler)
  api(libs.semVer)

  api(project(path = ":modulecheck-parsing:kotlin-compiler:api"))
  api(project(path = ":modulecheck-parsing:source:api"))
  api(project(path = ":modulecheck-utils:lazy"))

  implementation(project(path = ":modulecheck-name:api"))
  implementation(project(path = ":modulecheck-utils:coroutines:api"))

  testImplementation(libs.bundles.junit)
  testImplementation(libs.bundles.kotest)
  testImplementation(libs.kotlin.reflect)

  testImplementation(project(path = ":modulecheck-internal-testing"))
}