/*
 * Copyright (C) 2021 Rick Busarow
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

@file:Suppress("SpellCheckingInspection", "VariableNaming")

import modulecheck.builds.*

plugins {
  id("mcbuild.clean")
  id("mcbuild.detekt")
  id("mcbuild.java-library")
  id("mcbuild.kotlin")
  id("mcbuild.ktlint")
  id("mcbuild.test")
}

val GROUP = "com.rickbusarow.modulecheck"
val PLUGIN_ID = "com.rickbusarow.module-check"
val VERSION_NAME = "0.11.3"

val settings = extensions.create<ModuleCheckBuildExtension>(
  "mcbuild",
  ArtifactIdListener { onNewArtifactId(it) },
  DIListener { anvil, dagger ->
    applyAnvil(anvil = anvil, dagger = dagger)
    applyDagger(anvil = anvil, dagger = dagger)
  }
)

fun onNewArtifactId(artifactId: String) {
  project.configurePublishing(artifactId)
}

@Suppress("UnstableApiUsage")
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
