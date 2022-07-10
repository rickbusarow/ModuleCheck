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

package modulecheck.gradle

import modulecheck.finding.FindingName
import modulecheck.gradle.platforms.android.AgpApiAccess
import modulecheck.gradle.platforms.android.internal.generatesBuildConfig
import modulecheck.gradle.platforms.android.internal.isMissingManifestFile
import modulecheck.gradle.task.MultiRuleModuleCheckTask
import modulecheck.gradle.task.SingleRuleModuleCheckTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin

class ModuleCheckPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    val settings = target.extensions
      .create("moduleCheck", ModuleCheckExtension::class.java)

    val gradleVersion = target.gradle.gradleVersion.split('.')
      .let { segments ->
        // Gradle doesn't use semantic versioning, so for instance `7.4` is "7.4" and not "7.4.0".
        // Fortunately `7.0` was "7.0" and not "7".
        val major = segments[0]
        val minor = segments.getOrElse(1) { "0" }
        "$major.$minor"
      }

    val disableConfigCache = gradleVersion >= "7.4"

    val agpApiAccess = AgpApiAccess()

    target.registerTasks(
      name = "moduleCheckSortDependencies",
      findingName = FindingName("sort-dependencies"),
      includeAuto = true,
      disableConfigCache = disableConfigCache,
      agpApiAccess = agpApiAccess
    )
    target.registerTasks(
      name = "moduleCheckSortPlugins",
      findingName = FindingName("sort-plugins"),
      includeAuto = true,
      disableConfigCache = disableConfigCache,
      agpApiAccess = agpApiAccess
    )
    target.registerTasks(
      name = "moduleCheckDepths",
      findingName = FindingName("project-depth"),
      includeAuto = false,
      disableConfigCache = disableConfigCache,
      agpApiAccess = agpApiAccess,
      doFirstAction = {
        settings.checks.depths = true
        settings.reports.depths.enabled = true
      }
    )
    target.registerTasks(
      name = "moduleCheckGraphs",
      findingName = FindingName("project-depth"),
      includeAuto = false,
      disableConfigCache = disableConfigCache,
      agpApiAccess = agpApiAccess,
      doFirstAction = {
        settings.reports.graphs.enabled = true
      }
    )
    target.registerTasks(
      name = "moduleCheck",
      findingName = null,
      includeAuto = true,
      disableConfigCache = disableConfigCache,
      agpApiAccess = agpApiAccess
    )

    target.tasks
      .matching { it.name == LifecycleBasePlugin.CHECK_TASK_NAME }
      .configureEach {
        it.dependsOn("moduleCheck")
      }
  }

  @Suppress("LongParameterList")
  private fun Project.registerTasks(
    name: String,
    findingName: FindingName?,
    includeAuto: Boolean,
    disableConfigCache: Boolean,
    agpApiAccess: AgpApiAccess,
    doFirstAction: (() -> Unit)? = null
  ) {

    fun TaskProvider<*>.maybeAddDependencies() {
      configure { mcTask ->
        allprojects
          .filter { it.isMissingManifestFile(agpApiAccess) }
          .flatMap {
            it.tasks.withType(
              com.android.build.gradle.tasks.ManifestProcessorTask::class.java
            )
          }
          .forEach { mcTask.dependsOn(it) }

        allprojects
          .filter { it.generatesBuildConfig(agpApiAccess) }
          .flatMap {
            it.tasks.withType(
              com.android.build.gradle.tasks.GenerateBuildConfig::class.java
            )
          }
          .forEach { mcTask.dependsOn(it) }
      }
    }

    val tasks = if (findingName != null) {
      listOfNotNull(
        tasks.register(name, SingleRuleModuleCheckTask::class.java) {
          it.configure(
            findingName = findingName,
            autoCorrect = false,
            disableConfigCache = disableConfigCache
          )
        },
        if (includeAuto) {
          tasks.register("${name}Auto", SingleRuleModuleCheckTask::class.java) {
            it.configure(
              findingName = findingName,
              autoCorrect = true,
              disableConfigCache = disableConfigCache
            )
          }
        } else null
      )
    } else {
      listOfNotNull(
        tasks.register(name, MultiRuleModuleCheckTask::class.java) {
          it.configure(autoCorrect = false, disableConfigCache = disableConfigCache)
        },
        if (includeAuto) {
          tasks.register("${name}Auto", MultiRuleModuleCheckTask::class.java) {
            it.configure(autoCorrect = true, disableConfigCache = disableConfigCache)
          }
        } else null
      )
    }

    tasks.forEach { taskProvider ->
      taskProvider
        .also { if (doFirstAction != null) it.configure { it.doFirst { doFirstAction() } } }
        .maybeAddDependencies()
    }
  }
}
