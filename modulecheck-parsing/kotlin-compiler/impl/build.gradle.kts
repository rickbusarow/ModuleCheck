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
  id("com.google.devtools.ksp")
}

mcbuild {
  artifactId = "modulecheck-parsing-kotlin-compiler-impl"
  anvil = true
}

dependencies {

  api(libs.google.dagger.api)
  api(libs.kotlin.compiler)
  api(libs.kotlin.reflect)

  api(project(path = ":modulecheck-gradle:platforms:api"))
  api(project(path = ":modulecheck-parsing:gradle:model:api"))
  api(project(path = ":modulecheck-parsing:kotlin-compiler:api"))
  api(project(path = ":modulecheck-project:api"))
  api(project(path = ":modulecheck-utils:lazy"))

  compileOnly(gradleApi())

  compileOnly(libs.agp)

  implementation(libs.groovy)

  implementation(project(path = ":modulecheck-dagger"))
  implementation(project(path = ":modulecheck-utils:coroutines:api"))
  implementation(project(path = ":modulecheck-utils:stdlib"))

  testImplementation(libs.bundles.hermit)
  testImplementation(libs.bundles.jUnit)
  testImplementation(libs.bundles.kotest)
  implementation(libs.square.moshi)
  implementation(libs.square.moshi.kotlin)
  testImplementation(libs.kotest.runner)

  "ksp"(libs.square.moshi.codegen)
  "kspTest"(libs.square.moshi.codegen)

  testImplementation(project(path = ":modulecheck-parsing:gradle:model:api"))
  testImplementation(project(path = ":modulecheck-parsing:kotlin-compiler:api"))
  testImplementation(project(path = ":modulecheck-parsing:kotlin-compiler:impl"))
  testImplementation(project(path = ":modulecheck-project:testing"))
}
