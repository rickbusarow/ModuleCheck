/*
 * Copyright (C) 2021-2025 Rick Busarow
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
  alias(libs.plugins.mahout.kotlin.jvm.module)
}

mahout {
  publishing {
    publishMaven(artifactId = "modulecheck-parsing-kotlin-compiler-api")
  }
}

dependencies {
  api(libs.kotlin.compiler)
  api(libs.kotlin.reflect)

  compileOnly(gradleApi())

  compileOnly(libs.agp)

  implementation(project(path = ":modulecheck-utils:lazy"))

  testImplementation(libs.bundles.junit)
  testImplementation(libs.bundles.kotest)
  testImplementation(libs.kotest.runner.junit5.jvm)
}
