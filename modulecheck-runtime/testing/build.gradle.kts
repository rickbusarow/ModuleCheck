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
    artifactId = "modulecheck-runtime-testing"
  )
  anvil()
}

dependencies {

  api(libs.bundles.hermit)
  api(libs.kotlinx.coroutines.core)

  api(project(path = ":modulecheck-config:api"))
  api(project(path = ":modulecheck-config:impl"))
  api(project(path = ":modulecheck-dagger"))
  api(project(path = ":modulecheck-finding:api"))
  api(project(path = ":modulecheck-project:api"))
  api(project(path = ":modulecheck-project:testing"))
  api(project(path = ":modulecheck-reporting:checkstyle"))
  api(project(path = ":modulecheck-reporting:console"))
  api(project(path = ":modulecheck-reporting:graphviz"))
  api(project(path = ":modulecheck-reporting:logging:api"))
  api(project(path = ":modulecheck-reporting:logging:testing"))
  api(project(path = ":modulecheck-rule:api"))
  api(project(path = ":modulecheck-rule:impl-factory"))

  implementation(project(path = ":modulecheck-config:fake"))
  implementation(project(path = ":modulecheck-internal-testing"))
  implementation(project(path = ":modulecheck-reporting:sarif"))
  implementation(project(path = ":modulecheck-rule:testing"))
  implementation(project(path = ":modulecheck-runtime:api"))
  implementation(project(path = ":modulecheck-utils:stdlib"))
}
