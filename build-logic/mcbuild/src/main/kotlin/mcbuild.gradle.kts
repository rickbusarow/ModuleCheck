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

import modulecheck.builds.ModuleCheckBuildExtension
import modulecheck.builds.libsCatalog

plugins {
  id("mcbuild.clean")
  id("mcbuild.detekt")
  id("mcbuild.dependency-guard")
  id("mcbuild.dokka")
  id("mcbuild.java-library")
  id("mcbuild.kotlin")
  id("mcbuild.ktlint")
  id("mcbuild.test")

  id("com.google.devtools.ksp") apply false
}

val settings = extensions.create<ModuleCheckBuildExtension>("mcbuild")

val kotlinVersion = project.libsCatalog
  .findVersion("kotlin")
  .get()
  .requiredVersion

configurations.all {
  resolutionStrategy {
    eachDependency {
      if (requested.group == "org.jetbrains.kotlin") {
        useVersion(kotlinVersion)
      }
    }
  }
}
