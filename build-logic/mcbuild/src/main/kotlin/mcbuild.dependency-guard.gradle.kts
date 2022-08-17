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

plugins {
  kotlin("jvm")
  id("com.dropbox.dependency-guard")
}

dependencyGuard {
  configuration("runtimeClasspath") {
    modules = false
  }
}

// Delete any existing dependency files/directories before recreating with a new baseline task.
val dependencyGuardDeleteBaselines by tasks.registering(Delete::class) {
  delete("dependencies")
}

tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
  dependsOn("dependencyGuard")
}

tasks.named("dependencyGuardBaseline") task@{
  dependsOn(dependencyGuardDeleteBaselines)

  finalizedBy(rootProject.tasks.named("dependencyGuardAggregate"))
}

tasks.named("dependencyGuard") {
  dependsOn(rootProject.tasks.named("dependencyGuardExplode"))
  finalizedBy(dependencyGuardDeleteBaselines)
}
