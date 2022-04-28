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
  id("java-test-fixtures")
}

mcbuild {
  artifactId = "modulecheck-runtime"
  dagger = true
}

val isIdeSync = System.getProperty("idea.sync.active", "false").toBoolean()

dependencies {

  api(libs.kotlinx.coroutines.core)
  api(libs.rickBusarow.dispatch.core)

  api(project(path = ":modulecheck-api"))
  api(project(path = ":modulecheck-project:api"))
  api(project(path = ":modulecheck-reporting:checkstyle"))
  api(project(path = ":modulecheck-reporting:console"))
  api(project(path = ":modulecheck-reporting:graphviz"))
  api(project(path = ":modulecheck-reporting:sarif"))

  testFixturesApi(libs.bundles.hermit)

  testFixturesApi(project(path = ":modulecheck-core"))

  testFixturesApi(testFixtures(project(path = ":modulecheck-api")))
  testFixturesApi(testFixtures(project(path = ":modulecheck-project:api")))

  if (isIdeSync) {
    compileOnly(libs.bundles.hermit)
    compileOnly(libs.bundles.jUnit)
    compileOnly(libs.bundles.kotest)
  }
}
