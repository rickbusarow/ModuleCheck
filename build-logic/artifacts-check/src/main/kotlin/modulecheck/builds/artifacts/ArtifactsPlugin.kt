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

import modulecheck.builds.checkProjectIsRoot
import modulecheck.builds.matchingName
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.language.base.plugins.LifecycleBasePlugin

abstract class ArtifactsPlugin : Plugin<Project> {

  override fun apply(target: Project) {

    target.checkProjectIsRoot()

    target.tasks.register("artifactsDump", ArtifactsDumpTask::class.java)
    val artifactsCheck = target.tasks.register("artifactsCheck", ArtifactsCheckTask::class.java)

    target.tasks.matchingName(LifecycleBasePlugin.CHECK_TASK_NAME)
      .configureEach { task ->
        task.dependsOn(artifactsCheck)
      }

    target.allprojects {
      target.tasks.withType(AbstractPublishToMaven::class.java) {
        it.dependsOn(artifactsCheck)
      }
    }
  }
}
