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
  id("kotlinx-knit")
}

extensions.configure<kotlinx.knit.KnitPluginExtension> {

  rootDir = rootProject.rootDir

  files = fileTree(project.rootDir) {
    include(
      "**/*.md",
      "**/*.kt",
      "**/*.kts"
    )
    exclude(
      "**/node_modules/**",
      "**/build/**",
      "**/sample/**",
      "**/.gradle/**"
    )
  }

  moduleRoots = listOf(".")

  moduleDocs = "build/dokka/htmlMultiModule"
  dokkaMultiModuleRoot = "build/dokka/htmlMultiModule"
  moduleMarkers = listOf("build.gradle", "build.gradle.kts")
  siteRoot = "https://rbusarow.github.io/ModuleCheck/api/"
}

// Build API docs for all modules with dokka before running Knit
tasks.withType<kotlinx.knit.KnitTask>()
  .configureEach {
    notCompatibleWithConfigurationCache("")
    dependsOn("dokkaHtmlMultiModule")
  }
