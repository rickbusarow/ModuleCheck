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
import com.rickbusarow.kgx.EagerGradleApi
import com.rickbusarow.kgx.applyOnce
import com.rickbusarow.kgx.dependsOn
import com.rickbusarow.kgx.matchingName
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

    target.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME)
      .dependsOn(DEPENDENCY_GUARD_CHECK_TASK_NAME)

    // Delete any existing dependency files/directories before recreating with a new baseline task.
    val dependencyGuardDeleteBaselines = target.tasks
      .register("dependencyGuardDeleteBaselines", Delete::class.java) {
        it.delete("dependencies")
        it.mustRunAfter(DEPENDENCY_GUARD_BASELINE_TASK_NAME)
      }

    @OptIn(EagerGradleApi::class)
    target.tasks.named(DEPENDENCY_GUARD_BASELINE_TASK_NAME) {

      it.finalizedBy(
        target.rootProject.tasks.matchingName(DependencyGuardAggregatePlugin.AGGREGATE_TASK_NAME)
      )
    }
    @OptIn(EagerGradleApi::class)
    target.tasks.named(DEPENDENCY_GUARD_CHECK_TASK_NAME) {
      it.dependsOn(
        target.rootProject.tasks.matchingName(DependencyGuardAggregatePlugin.EXPLODE_TASK_NAME)
      )
      it.finalizedBy(dependencyGuardDeleteBaselines)
    }
  }

  companion object {
    const val DEPENDENCY_GUARD_CHECK_TASK_NAME = "dependencyGuard"
    const val DEPENDENCY_GUARD_BASELINE_TASK_NAME = "dependencyGuardBaseline"
  }
}
