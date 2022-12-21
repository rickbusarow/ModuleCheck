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

package modulecheck.builds

import modulecheck.builds.dependencyGuardAggregate.DependencyGuardAggregateTask
import modulecheck.builds.dependencyGuardAggregate.DependencyGuardExplodeTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

abstract class DependencyGuardAggregatePlugin : Plugin<Project> {
  override fun apply(target: Project) {

    target.checkProjectIsRoot()

    target.tasks.register(
      "dependencyGuardAggregate",
      DependencyGuardAggregateTask::class.java
    ) { task ->
      task.rootDir.set(target.rootDir)

      target.subprojects.forEach { subProject ->
        task.source(subProject.fileTree(subProject.file("dependencies")))
      }

      task.outputFile.set(target.file("dependency-guard-aggregate.txt"))
      target.allprojects.forEach { proj ->
        task.dependsOn(proj.tasks.matchingName("dependencyGuardBaseline"))
      }
    }

    target.tasks.register(
      "dependencyGuardExplode",
      DependencyGuardExplodeTask::class.java
    ) { task ->
      task.aggregateFile.set(target.file("dependency-guard-aggregate.txt"))
      task.rootDir.set(target.rootDir)
    }
    target.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) { task ->
      task.dependsOn(target.tasks.matchingName("dependencyGuard"))
    }
  }
}
