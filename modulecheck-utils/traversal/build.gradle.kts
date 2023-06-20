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
  published(
    artifactId = "modulecheck-utils-traversal"
  )
}

dependencies {

  api(libs.kotlinx.coroutines.core)
  api(libs.kotlinx.coroutines.jdk8)
  api(libs.kotlinx.coroutines.jvm)

  implementation(project(":modulecheck-utils:stdlib"))

  testImplementation(libs.bundles.junit)
  testImplementation(libs.bundles.kotest)
  testImplementation(libs.square.turbine)
}
