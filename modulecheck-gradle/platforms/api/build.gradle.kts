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
    artifactId = "modulecheck-gradle-platform-api"
  )
  anvil()
}

dependencies {

  api(libs.javax.inject)
  api(libs.kotlin.compiler)
  api(libs.semVer)

  api(project(path = ":modulecheck-model:dependency:api"))
  api(project(path = ":modulecheck-model:sourceset:api"))
  api(project(path = ":modulecheck-parsing:gradle:model:api"))
  api(project(path = ":modulecheck-parsing:kotlin-compiler:api"))
  api(project(path = ":modulecheck-utils:lazy"))

  compileOnly(gradleApi())

  compileOnly(libs.agp)
  compileOnly(libs.agp.api)
  compileOnly(libs.agp.builder.model)
  compileOnly(libs.kotlin.gradle.plugin)
  compileOnly(libs.kotlin.gradle.plugin.api)

  implementation(libs.kotlin.reflect)

  implementation(project(path = ":modulecheck-dagger"))

  testImplementation(libs.bundles.hermit)
  testImplementation(libs.bundles.junit)
  testImplementation(libs.bundles.kotest)
}
