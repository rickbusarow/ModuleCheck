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

import modulecheck.builds.DependencyGuardConventionPlugin.Companion.DEPENDENCY_GUARD_BASELINE_TASK_NAME
import modulecheck.builds.DependencyGuardConventionPlugin.Companion.DEPENDENCY_GUARD_CHECK_TASK_NAME
import modulecheck.builds.dependencyGuardAggregate.DependencyGuardAggregateTask
import modulecheck.builds.dependencyGuardAggregate.DependencyGuardExplodeTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Stores all the `dependencies/runtimeClasspath.txt` files from
 * individual modules in a single .txt file in the root project directory.
 *
 * The individual files are recreated before the real dependency-guard plugin does its comparisons.
 */
abstract class DependencyGuardAggregatePlugin : Plugin<Project> {
  override fun apply(target: Project) {

    target.checkProjectIsRoot()

    val aggregateFile = target.file("dependency-guard-aggregate.txt")

    target.tasks.register(AGGREGATE_TASK_NAME, DependencyGuardAggregateTask::class.java) { task ->
      task.rootDir.set(target.rootDir)

      target.subprojects.forEach { subProject ->
        task.source(subProject.fileTree(subProject.file("dependencies")))
        task.finalizedBy(subProject.tasks.matchingName("dependencyGuardDeleteBaselines"))
      }

      task.outputFile.set(aggregateFile)

      task.mustRunAfter(target.allProjectsTasksMatchingName(DEPENDENCY_GUARD_CHECK_TASK_NAME))
      task.dependsOn(target.allProjectsTasksMatchingName(DEPENDENCY_GUARD_BASELINE_TASK_NAME))
    }

    target.tasks.named("fix").dependsOn(AGGREGATE_TASK_NAME)

    target.tasks.register(EXPLODE_TASK_NAME, DependencyGuardExplodeTask::class.java) { task ->
      task.aggregateFile.set(aggregateFile)
      task.rootDir.set(target.rootDir)
    }

    target.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) { task ->
      task.dependsOn(target.tasks.matchingName(EXPLODE_TASK_NAME))
    }
  }

  companion object {
    const val AGGREGATE_TASK_NAME = "dependencyGuardAggregate"
    const val EXPLODE_TASK_NAME = "dependencyGuardExplode"
  }
}
