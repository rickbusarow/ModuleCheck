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
  artifactId = "modulecheck-core"
  anvil()

  buildProperties(
    "test",
    """
    package modulecheck.core

    internal class BuildProperties {
      val websiteDir = "${rootDir.resolve("website").invariantSeparatorsPath}"
    }
    """
  )
}

dependencies {

  api(libs.kotlinx.coroutines.core)
  api(libs.rickBusarow.dispatch.core)

  api(project(path = ":modulecheck-finding:impl"))
  api(project(path = ":modulecheck-model:dependency:api"))
  api(project(path = ":modulecheck-model:sourceset:api"))
  api(project(path = ":modulecheck-project:api"))
  api(project(path = ":modulecheck-utils:cache"))

  implementation(libs.kotlinx.coroutines.jvm)
  implementation(libs.semVer)

  implementation(project(path = ":modulecheck-api"))
  implementation(project(path = ":modulecheck-finding:name"))
  implementation(project(path = ":modulecheck-utils:coroutines:api"))
  implementation(project(path = ":modulecheck-utils:lazy"))
  implementation(project(path = ":modulecheck-utils:stdlib"))

  testImplementation(libs.bundles.hermit)
  testImplementation(libs.bundles.junit)
  testImplementation(libs.bundles.kotest)
  testImplementation(libs.rickBusarow.dispatch.test.core)

  testImplementation(project(path = ":modulecheck-config:api"))
  testImplementation(project(path = ":modulecheck-config:fake"))
  testImplementation(project(path = ":modulecheck-finding:api"))
  testImplementation(project(path = ":modulecheck-internal-testing"))
  testImplementation(project(path = ":modulecheck-parsing:source:api"))
  testImplementation(project(path = ":modulecheck-project-generation:api"))
  testImplementation(project(path = ":modulecheck-rule:impl"))
  testImplementation(project(path = ":modulecheck-rule:impl-factory"))
  testImplementation(project(path = ":modulecheck-runtime:testing"))
  testImplementation(project(path = ":modulecheck-utils:stdlib"))
}
