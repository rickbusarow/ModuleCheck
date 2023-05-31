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

import com.dropbox.gradle.plugins.dependencyguard.DependencyGuardPluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.language.base.plugins.LifecycleBasePlugin

abstract class DependencyGuardConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {

    if (target == target.rootProject) {
      return
    }

    target.plugins.applyOnce("org.jetbrains.kotlin.jvm")
    target.plugins.applyOnce("com.dropbox.dependency-guard")

    target.extensions.configure(DependencyGuardPluginExtension::class.java) { extension ->
      extension.configuration("runtimeClasspath") {
        it.modules = false
      }
    }

    // Delete any existing dependency files/directories before recreating with a new baseline task.
    val dependencyGuardDeleteBaselines = target.tasks.register(
      "dependencyGuardDeleteBaselines",
      Delete::class.java
    ) {
      it.delete("dependencies")
      it.mustRunAfter("dependencyGuardBaseline")
    }

    target.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure {
      it.dependsOn("dependencyGuard")
    }

    target.tasks.named("dependencyGuardBaseline").configure {
      it.finalizedBy(target.rootProject.tasks.matchingName("dependencyGuardAggregate"))
    }
    target.tasks.named("dependencyGuard").configure {
      it.dependsOn(target.rootProject.tasks.matchingName("dependencyGuardExplode"))
      it.finalizedBy(dependencyGuardDeleteBaselines)
    }
  }
}
