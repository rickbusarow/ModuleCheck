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
    artifactId = "modulecheck-project-generation-api"
  )
  anvil()
}

dependencies {

  api(project(path = ":modulecheck-config:api"))
  api(project(path = ":modulecheck-model:dependency:api"))
  api(project(path = ":modulecheck-model:dependency:impl"))
  api(project(path = ":modulecheck-model:sourceset:api"))
  api(project(path = ":modulecheck-parsing:gradle:dsl:api"))
  api(project(path = ":modulecheck-parsing:kotlin-compiler:impl"))
  api(project(path = ":modulecheck-parsing:source:api"))
  api(project(path = ":modulecheck-project:api"))
  api(project(path = ":modulecheck-reporting:logging:api"))
  api(project(path = ":modulecheck-utils:lazy"))

  compileOnly(gradleApi())

  implementation(libs.bundles.junit)
  implementation(libs.bundles.kotest)
  implementation(libs.bundles.kotest)
  implementation(libs.kotlin.reflect)

  implementation(project(path = ":modulecheck-api"))
  implementation(project(path = ":modulecheck-internal-testing"))
  implementation(project(path = ":modulecheck-parsing:gradle:dsl:internal"))
  implementation(project(path = ":modulecheck-parsing:gradle:dsl:precompiled"))
  implementation(project(path = ":modulecheck-parsing:groovy-antlr"))
  implementation(project(path = ":modulecheck-parsing:kotlin-compiler:api"))
  implementation(project(path = ":modulecheck-parsing:psi"))
  implementation(project(path = ":modulecheck-parsing:wiring"))
  implementation(project(path = ":modulecheck-project:impl"))
  implementation(project(path = ":modulecheck-utils:stdlib"))
}
