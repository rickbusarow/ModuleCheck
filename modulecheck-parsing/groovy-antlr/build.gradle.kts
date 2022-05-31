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
  groovy
}

mcbuild {
  artifactId = "modulecheck-parsing-groovy-antlr"
}

dependencies {

  api(project(path = ":modulecheck-model:dependency:api"))
  api(project(path = ":modulecheck-parsing:gradle:dsl:api"))
  api(project(path = ":modulecheck-parsing:gradle:dsl:internal"))
  api(project(path = ":modulecheck-parsing:gradle:model:api"))
  api(project(path = ":modulecheck-parsing:gradle:model:api-dependency"))
  api(project(path = ":modulecheck-reporting:logging:api"))

  compileOnly(gradleApi())

  implementation(libs.antlr.core)
  implementation(libs.antlr.runtime)
  implementation(libs.groovy)
  implementation(libs.kotlin.compiler)
  implementation(libs.kotlin.reflect)

  implementation(project(path = ":modulecheck-utils:stdlib"))

  testImplementation(libs.bundles.hermit)
  testImplementation(libs.bundles.jUnit)
  testImplementation(libs.bundles.kotest)

  testImplementation(project(path = ":modulecheck-internal-testing"))
  testImplementation(project(path = ":modulecheck-parsing:gradle:dsl:testing"))
  testImplementation(project(path = ":modulecheck-reporting:logging:api"))
}
