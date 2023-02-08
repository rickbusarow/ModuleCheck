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

package modulecheck.builds

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask
import java.net.URL

abstract class DokkaConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {

    target.plugins.applyOnce("org.jetbrains.dokka")

    target.tasks.withType(AbstractDokkaLeafTask::class.java) { task ->

      // Dokka doesn't support configuration caching
      task.notCompatibleWithConfigurationCache("Dokka doesn't support configuration caching")

      // Dokka uses their outputs but doesn't explicitly depend upon them.
      task.mustRunAfter(target.tasks.withType(KotlinCompile::class.java))
      task.mustRunAfter(target.tasks.withType(LintTask::class.java))
      task.mustRunAfter(target.tasks.withType(FormatTask::class.java))

      // The default moduleName for each module in the module list is its unqualified "name",
      // meaning the list would be full of "api", "impl", etc.  Instead, use the module's maven
      // artifact ID, if it has one, or default to its full Gradle path for internal modules.
      val fullModuleName = target.extensions.findByType(ArtifactIdExtension::class.java)
        ?.artifactId
        ?: target.path.removePrefix(":")
      task.moduleName.set(fullModuleName)

      if (target != target.rootProject) {
        task.dokkaSourceSets.getByName("main") { builder ->

          builder.samples.setFrom(
            target.fileTree(target.projectDir) { tree ->
              tree.include("samples/**")
            }
          )

          val readmeFile = target.file("${target.projectDir}/README.md")

          if (readmeFile.exists()) {
            builder.includes.from(readmeFile)
          }

          builder.sourceLink { sourceLinkBuilder ->
            sourceLinkBuilder.localDirectory.set(target.file("src/main"))

            val modulePath = target.path.replace(":", "/")
              .replaceFirst("/", "")

            // URL showing where the source code can be accessed through the web browser
            sourceLinkBuilder.remoteUrl.set(
              URL("https://github.com/RBusarow/ModuleCheck/blob/main/$modulePath/src/main")
            )
            // Suffix which is used to append the line number to the URL. Use #L for GitHub
            sourceLinkBuilder.remoteLineSuffix.set("#L")
          }
        }
      }
    }
  }
}
