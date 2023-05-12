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

import modulecheck.builds.shards.registerYamlShardsTasks
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask
import java.net.URL

abstract class DokkaConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {

    target.plugins.applyOnce("org.jetbrains.dokka")

    target.tasks.withType(AbstractDokkaLeafTask::class.java).configureEach { task ->

      // Dokka doesn't support configuration caching
      task.notCompatibleWithConfigurationCache("Dokka doesn't support configuration caching")

      // Dokka uses their outputs but doesn't explicitly depend upon them.
      task.mustRunAfter(target.tasks.withType(KotlinCompile::class.java))
      task.mustRunAfter(target.tasks.withType(LintTask::class.java))
      task.mustRunAfter(target.tasks.withType(FormatTask::class.java))

      // The default moduleName for each module in the module list is its unqualified "name",
      // meaning the list would be full of "api", "impl", etc.  Instead, use the module's maven
      // artifact ID, if it has one, or default to its full Gradle path for internal modules.
      val fullModuleName = target.artifactId ?: target.path.removePrefix(":")
      task.moduleName.set(fullModuleName)

      if (!target.isRootProject()) {
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

    if (target.isRootProject()) {

      @Suppress("MagicNumber")
      val shardCount = 5

      target.registerYamlShardsTasks(
        shardCount = shardCount,
        startTagName = "### <start-dokka-partial-shards>",
        endTagName = "### <end-dokka-partial-shards>",
        taskNamePart = "dokkaHtmlPartial"
      )

      // Assign each project to a shard.
      // It's lazy so that the work only happens at task configuration time, but it's outside the
      // task configuration block so that it only happens once.
      val shardAssignments by lazy {

        // count how many project dependencies each project has
        val projectCosts = target.subprojects
          .associateWith { project ->
            project
              .configurations
              .flatMap { it.dependencies.filterIsInstance<ProjectDependency>() }
              .size
          }

        // Sort the projects by descending cost, then fall back to the project paths The path sort
        // is just so that the shard composition is stable.  If the shard composition isn't
        // stable, the shard tasks may not be up-to-date and build caching in CI is broken.
        val sortedProjects = projectCosts.keys
          .sortedWith(compareBy(
            { projectCosts.getValue(it) },
            { it.path }
          ))

        var shardIndex = 0

        sortedProjects.groupBy { (shardIndex++ % shardCount) + 1 }
      }

      (1..shardCount).map { shardIndex ->

        target.tasks.register(
          "dokkaHtmlPartialShard$shardIndex",
          ModuleCheckBuildTask::class.java
        ) { task ->

          val assignedTasks = shardAssignments
            .getValue(shardIndex)
            .map { project ->
              project.tasks.matchingName("dokkaHtmlPartial")
            }

          task.dependsOn(assignedTasks)
        }
      }
    }
  }
}
