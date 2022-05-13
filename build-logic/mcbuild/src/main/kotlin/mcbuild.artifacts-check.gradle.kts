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

import modulecheck.builds.artifacts.ArtifactsCheckTask
import modulecheck.builds.artifacts.ArtifactsDumpTask

check(project.rootProject == project) {
  "Only apply this plugin to the project root."
}

val artifactsDump by tasks.registering(ArtifactsDumpTask::class)

val artifactsCheck by tasks.registering(ArtifactsCheckTask::class)

// Automatically run `artifactsCheck` when running `check`
tasks
  .matching { it.name == LifecycleBasePlugin.CHECK_TASK_NAME }
  .configureEach {
    dependsOn(artifactsCheck)
  }

// Before any publishing task (local or remote) ensure that the artifacts check is executed.
allprojects {
  tasks.withType(AbstractPublishToMaven::class.java) {
    dependsOn(artifactsCheck)
  }
}
