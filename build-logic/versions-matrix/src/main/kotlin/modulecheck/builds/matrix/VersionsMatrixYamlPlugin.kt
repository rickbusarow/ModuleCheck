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

package modulecheck.builds.matrix

import modulecheck.builds.applyOnce
import modulecheck.builds.checkProjectIsRoot
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

abstract class VersionsMatrixYamlPlugin : Plugin<Project> {
  override fun apply(target: Project) {

    target.checkProjectIsRoot()
    target.plugins.applyOnce("base")

    val ciFile = target.file(".github/workflows/ci.yml")

    require(ciFile.exists()) {
      "Could not resolve '$ciFile'.  Only add the ci/yaml matrix tasks to the root project."
    }

    val versionsMatrixYamlCheck = target.tasks.register(
      "versionsMatrixYamlCheck",
      VersionsMatrixYamlCheckTask::class.java
    ) { task ->
      task.yamlFile.set(ciFile)
    }

    // Automatically run `versionsMatrixYamlCheck` when running `check`
    target.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) { it.dependsOn(versionsMatrixYamlCheck) }

    target.tasks.register(
      "versionsMatrixGenerateYaml",
      VersionsMatrixYamlGenerateTask::class.java
    ) {
      it.yamlFile.set(ciFile)
    }
  }
}
