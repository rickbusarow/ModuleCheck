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
    artifactId = "modulecheck-parsing-psi"
  )
}

dependencies {

  api(libs.javax.inject)
  api(libs.kotlin.compiler)

  api(project(path = ":modulecheck-model:dependency:api"))
  api(project(path = ":modulecheck-model:dependency:api"))
  api(project(path = ":modulecheck-model:sourceset:api"))
  api(project(path = ":modulecheck-parsing:gradle:dsl:api"))
  api(project(path = ":modulecheck-parsing:gradle:dsl:internal"))
  api(project(path = ":modulecheck-parsing:kotlin-compiler:api"))
  api(project(path = ":modulecheck-parsing:kotlin-compiler:api"))
  api(project(path = ":modulecheck-parsing:kotlin-compiler:api"))
  api(project(path = ":modulecheck-parsing:source:api"))
  api(project(path = ":modulecheck-project:api"))
  api(project(path = ":modulecheck-reporting:logging:api"))
  api(project(path = ":modulecheck-utils:lazy"))
  api(project(path = ":modulecheck-utils:traversal"))

  compileOnly(libs.kotlin.reflect)

  implementation(project(path = ":modulecheck-model:dependency:api"))
  implementation(project(path = ":modulecheck-parsing:gradle:dsl:precompiled"))
  implementation(project(path = ":modulecheck-parsing:kotlin-compiler:api"))
  implementation(project(path = ":modulecheck-parsing:kotlin-compiler:api"))
  implementation(project(path = ":modulecheck-utils:stdlib"))

  testImplementation(libs.bundles.junit)
  testImplementation(libs.bundles.kotest)
  testImplementation(libs.kotest.runner.junit5.jvm)

  testImplementation(project(path = ":modulecheck-api"))
  testImplementation(project(path = ":modulecheck-internal-testing"))
  testImplementation(project(path = ":modulecheck-parsing:gradle:dsl:testing"))
  testImplementation(project(path = ":modulecheck-parsing:psi"))
  testImplementation(project(path = ":modulecheck-parsing:source:testing"))
  testImplementation(project(path = ":modulecheck-project:testing"))
}
