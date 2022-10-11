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

import modulecheck.builds.matrix.VersionsMatrixYamlCheckTask
import modulecheck.builds.matrix.VersionsMatrixYamlGenerateTask

val ciFile = rootProject.file(".github/workflows/ci.yml")

require(ciFile.exists()) {
  "Could not resolve '$ciFile'.  Only add the ci/yaml matrix tasks to the root project."
}

val versionsMatrixYamlCheck by tasks.registering(VersionsMatrixYamlCheckTask::class) {
  yamlFile.set(ciFile)
}

// Automatically run `versionsMatrixYamlCheck` when running `check`
tasks
  .matching { it.name == LifecycleBasePlugin.CHECK_TASK_NAME }
  .configureEach {
    dependsOn(versionsMatrixYamlCheck)
  }

tasks.register("versionsMatrixGenerateYaml", VersionsMatrixYamlGenerateTask::class) {
  yamlFile.set(ciFile)
}
