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

import modulecheck.builds.dependencyGuardAggregate.DependencyGuardAggregateTask
import modulecheck.builds.dependencyGuardAggregate.DependencyGuardExplodeTask
import modulecheck.builds.matchingName

plugins {
  kotlin("jvm")
}

tasks.register("dependencyGuardAggregate", DependencyGuardAggregateTask::class) task@{

  rootDir.set(project.rootDir)

  subprojects.forEach { sub ->

    source(sub.fileTree(sub.file("dependencies")))
  }

  outputFile.set(project.file("dependency-guard-aggregate.txt"))

  allprojects sub@{

    this@task.dependsOn(this@sub.tasks.matchingName("dependencyGuardBaseline"))
  }
}
tasks.register("dependencyGuardExplode", DependencyGuardExplodeTask::class) {

  aggregateFile.set(project.file("dependency-guard-aggregate.txt"))
  rootDir.set(project.rootDir)
}

tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
  dependsOn(tasks.matchingName("dependencyGuard"))
}
