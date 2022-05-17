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
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask

plugins {
  id("org.jetbrains.dokka")
}

// Dokka doesn't support configuration caching
tasks.withType(AbstractDokkaLeafTask::class.java) {
  notCompatibleWithConfigurationCache("")
}

subprojects {

  val proj = this

  val includeSubproject = when {
    path == ":modulecheck-internal-testing" -> false
    path == ":modulecheck-specs" -> false
    else -> File("${proj.projectDir}/src").exists()
  }

  if (includeSubproject) {
    apply(plugin = "org.jetbrains.dokka")

    proj.tasks
      .withType<AbstractDokkaLeafTask>()
      .configureEach {

        // Dokka doesn't support configuration caching
        notCompatibleWithConfigurationCache("")

        // Dokka uses their outputs but doesn't explicitly depend upon them.
        mustRunAfter(allprojects.map { it.tasks.withType(KotlinCompile::class.java) })
        mustRunAfter(allprojects.map { it.tasks.withType(KtLintCheckTask::class.java) })
        mustRunAfter(allprojects.map { it.tasks.withType(KtLintFormatTask::class.java) })

        proj.extensions.configure<ModuleCheckBuildExtension> {

          // The default moduleName for each module in the module list is its unqualified "name",
          // meaning the list would be full of "api", "impl", etc.  Instead, use the module's maven
          // artifact ID, if it has one, or default to its full Gradle path for internal modules.
          moduleName.set(artifactId ?: proj.path.removePrefix(":"))
        }

        dokkaSourceSets {

          getByName("main") {

            samples.setFrom(
              fileTree(proj.projectDir) {
                include("samples/**")
              }
            )

            if (File("${proj.projectDir}/README.md").exists()) {
              includes.from(files("${proj.projectDir}/README.md"))
            }

            sourceLink {
              localDirectory.set(file("src/main"))

              val modulePath = proj.path.replace(":", "/").replaceFirst("/", "")

              // URL showing where the source code can be accessed through the web browser
              remoteUrl.set(uri("https://github.com/RBusarow/ModuleCheck/blob/main/$modulePath/src/main").toURL())
              // Suffix which is used to append the line number to the URL. Use #L for GitHub
              remoteLineSuffix.set("#L")
            }
          }
        }
      }
  }
}
