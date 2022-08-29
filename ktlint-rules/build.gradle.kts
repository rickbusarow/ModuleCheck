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

import modulecheck.builds.DOCS_WEBSITE
import modulecheck.builds.SOURCE_WEBSITE
import modulecheck.builds.VERSION_NAME

plugins {
  id("mcbuild")
}

mcbuild {
  ksp()

  buildProperties(
    "main",
    """
    package modulecheck.builds.ktlint

    internal class BuildProperties {
      val version = "$VERSION_NAME"
      val sourceWebsite = "$SOURCE_WEBSITE"
      val docsWebsite = "$DOCS_WEBSITE"
    }
    """
  )
}

dependencies {
  api(libs.detekt.api)

  implementation(libs.google.auto.common)
  implementation(libs.google.auto.service.annotations)
  implementation(libs.google.ksp)
  implementation(libs.kotlin.compiler)
  implementation(libs.ktlint.core)
  implementation(libs.ktlint.gradle)
  implementation(libs.ktlint.ruleset.standard)

  ksp(libs.zacSweers.auto.service.ksp)

  testImplementation(libs.bundles.hermit)
  testImplementation(libs.bundles.jUnit)
  testImplementation(libs.bundles.kotest)
  testImplementation(libs.ktlint.test)
}

val jarTask = tasks.withType<Jar>()
rootProject.tasks.named("prepareKotlinBuildScriptModel") {
  dependsOn(jarTask)
}
