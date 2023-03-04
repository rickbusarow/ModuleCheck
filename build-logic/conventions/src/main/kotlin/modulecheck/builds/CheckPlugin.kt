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
import org.gradle.language.base.plugins.LifecycleBasePlugin

abstract class CheckPlugin : Plugin<Project> {
  override fun apply(target: Project) {

    target.plugins.applyOnce("base")

    val fix = target.tasks.register("fix", ModuleCheckBuildTask::class.java) { task ->

      task.group = "Verification"
      task.description = "Runs all auto-fix linting tasks"

      task.dependsOn(target.rootProject.tasks.matchingName("artifactsDump"))
      task.dependsOn(target.rootProject.tasks.matchingName("spotlessApply"))
      task.dependsOn(target.rootProject.tasks.matchingName("updateChangelogVersionLinks"))
      task.dependsOn(target.tasks.matchingName("apiDump"))
      task.dependsOn(target.tasks.matchingName("dependencyGuardBaseline"))
      task.dependsOn(target.tasks.matchingName("ktlintFormat"))
      task.dependsOn(target.tasks.matchingName("deleteEmptyDirs"))
      task.dependsOn(target.tasks.matchingName("moduleCheckAuto"))
    }

    // This is a convenience task which applies all available fixes before running `check`. Each
    // of the fixable linters use `mustRunAfter` to ensure that their auto-fix task runs before their
    // check-only task.
    target.tasks.register("checkFix", ModuleCheckBuildTask::class.java) { task ->

      task.group = "Verification"
      task.description = "Runs all auto-fix linting tasks, then runs all of the normal :check task"

      task.dependsOn(target.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME))
      task.dependsOn(fix)
    }
  }
}
