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

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.rickbusarow.kgx.applyOnce
import com.rickbusarow.kgx.checkProjectIsRoot
import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class BenManesVersionsPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    target.plugins.applyOnce("com.github.ben-manes.versions")

    target.checkProjectIsRoot()

    target.tasks.withType(
      DependencyUpdatesTask::class.java
    ).configureEach { task ->
      task.rejectVersionIf {
        isNonStable(it.candidate.version) && !isNonStable(it.currentVersion)
      }
    }
  }

  private fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA")
      .any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
  }
}
