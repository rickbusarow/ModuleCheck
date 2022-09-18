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

import modulecheck.builds.ArtifactIdExtension
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

plugins {
  id("org.jetbrains.dokka")
}

tasks.withType<AbstractDokkaLeafTask>()
  .configureEach {

    // Dokka doesn't support configuration caching
    notCompatibleWithConfigurationCache("Dokka doesn't support configuration caching")

    // Dokka uses their outputs but doesn't explicitly depend upon them.
    mustRunAfter(tasks.withType(KotlinCompile::class.java))
    mustRunAfter(tasks.withType(LintTask::class.java))
    mustRunAfter(tasks.withType(FormatTask::class.java))

    // The default moduleName for each module in the module list is its unqualified "name",
    // meaning the list would be full of "api", "impl", etc.  Instead, use the module's maven
    // artifact ID, if it has one, or default to its full Gradle path for internal modules.
    val fullModuleName = extensions.findByType<ArtifactIdExtension>()?.artifactId
      ?: project.path.removePrefix(":")
    moduleName.set(fullModuleName)

    if (project != rootProject) {
      dokkaSourceSets {

        getByName("main") {

          samples.setFrom(
            fileTree(projectDir) {
              include("samples/**")
            }
          )

          val readmeFile = file("$projectDir/README.md")

          if (readmeFile.exists()) {
            includes.from(readmeFile)
          }

          sourceLink {
            localDirectory.set(file("src/main"))

            val modulePath = project.path.replace(":", "/")
              .replaceFirst("/", "")

            // URL showing where the source code can be accessed through the web browser
            remoteUrl.set(uri("https://github.com/RBusarow/ModuleCheck/blob/main/$modulePath/src/main").toURL())
            // Suffix which is used to append the line number to the URL. Use #L for GitHub
            remoteLineSuffix.set("#L")
          }
        }
      }
    }
  }
