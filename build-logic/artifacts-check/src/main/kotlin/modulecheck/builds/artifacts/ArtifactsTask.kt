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

package modulecheck.builds.artifacts

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile

abstract class ArtifactsTask(
  private val projectLayout: ProjectLayout
) : DefaultTask() {

  /**
   * This file contains all definitions for published artifacts.
   *
   * It's located at the root of the project, assuming that the task is run from the root project.
   */
  @get:OutputFile
  protected val reportFile: RegularFile by lazy {
    projectLayout.projectDirectory.file("artifacts.json")
  }

  /**
   * All artifacts as they are defined in the project right now.
   *
   * This is a lazy delegate because it's accessing [project], and Gradle's configuration caching
   * doesn't allow direct references to `project` in task properties or inside task actions.
   * Somehow, it doesn't complain about this even though it's definitely accessed at runtime.
   */
  @get:Internal
  protected val currentList by lazy { project.createArtifactList() }

  @OptIn(ExperimentalStdlibApi::class)
  @get:Internal
  protected val moshiAdapter: JsonAdapter<List<ArtifactConfig>> by lazy {

    Moshi.Builder()
      .build()
      .adapter()
  }

  private fun Project.createArtifactList(): List<ArtifactConfig> {
    val map = subprojects
      .mapNotNull { sub ->

        var group: String? = null
        var artifactId: String? = null
        var pomDescription: String? = null
        var packaging: String? = null

        sub.extensions.findByType(PublishingExtension::class.java)
          ?.publications
          ?.findByName("maven")
          ?.let { it as? MavenPublication }
          ?.let { publication ->
            packaging = publication.pom.packaging
            group = publication.groupId
            artifactId = publication.artifactId
            pomDescription = publication.pom.description.orNull
          }

        listOfNotNull(group, artifactId, pomDescription, packaging)
          .also { allProperties ->

            require(allProperties.isEmpty() || allProperties.size == 4) {
              "expected all properties to be null or none to be null for project `${sub.path}, " +
                "but got:\n" +
                "group : $group\n" +
                "artifactId : $artifactId\n" +
                "pom description : $pomDescription\n" +
                "packaging : $packaging"
            }
          }
          .takeIf { it.size == 4 }
          ?.let { (group, artifactId, pomDescription, packaging) ->

            val javaVersion = sub.extensions.getByType(JavaPluginExtension::class.java)
              .sourceCompatibility
              .toString()

            ArtifactConfig(
              gradlePath = sub.path,
              group = group,
              artifactId = artifactId,
              description = pomDescription,
              packaging = packaging,
              javaVersion = javaVersion
            )
          }
      }

    return map
  }
}
