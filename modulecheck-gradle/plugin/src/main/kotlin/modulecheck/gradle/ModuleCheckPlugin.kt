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

import com.android.build.gradle.tasks.GenerateBuildConfig
import com.android.build.gradle.tasks.ManifestProcessorTask
import modulecheck.core.rule.DepthRule
import modulecheck.core.rule.ModuleCheckRuleFactory
import modulecheck.core.rule.MultiRuleFindingFactory
import modulecheck.core.rule.SingleRuleFindingFactory
import modulecheck.core.rule.SortDependenciesRule
import modulecheck.core.rule.SortPluginsRule
import modulecheck.gradle.platforms.internal.generatesBuildConfig
import modulecheck.gradle.platforms.internal.isMissingManifestFile
import modulecheck.gradle.task.ModuleCheckTask
import modulecheck.rule.FindingFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin

class ModuleCheckPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    val settings = target.extensions
      .create("moduleCheck", ModuleCheckExtension::class.java)

    val factory = ModuleCheckRuleFactory()

    val rules = factory.create(settings)

    val gradleVersion = target.gradle.gradleVersion.split('.')
      .let { segments ->
        // Gradle doesn't use semantic versioning, so for instance `7.4` is "7.4" and not "7.4.0".
        // Fortunately `7.0` was "7.0" and not "7".
        val major = segments[0]
        val minor = segments.getOrElse(1) { "0" }
        "$major.$minor"
      }

    val disableConfigCache = gradleVersion >= "7.4"

    target.registerTasks(
      name = "moduleCheckSortDependencies",
      findingFactory = SingleRuleFindingFactory(SortDependenciesRule(settings)),
      includeAuto = true,
      disableConfigCache = disableConfigCache
    )
    target.registerTasks(
      name = "moduleCheckSortPlugins",
      findingFactory = SingleRuleFindingFactory(SortPluginsRule(settings)),
      includeAuto = true,
      disableConfigCache = disableConfigCache
    )
    target.registerTasks(
      name = "moduleCheckDepths",
      findingFactory = SingleRuleFindingFactory(DepthRule()),
      includeAuto = false,
      disableConfigCache = disableConfigCache,
      doFirstAction = {
        settings.checks.depths = true
        settings.reports.depths.enabled = true
      }
    )
    target.registerTasks(
      name = "moduleCheckGraphs",
      findingFactory = SingleRuleFindingFactory(DepthRule()),
      includeAuto = false,
      disableConfigCache = disableConfigCache,
      doFirstAction = {
        settings.reports.graphs.enabled = true
      }
    )
    target.registerTasks(
      name = "moduleCheck",
      findingFactory = MultiRuleFindingFactory(settings, rules),
      includeAuto = true,
      disableConfigCache = disableConfigCache
    )

    target.tasks
      .matching { it.name == LifecycleBasePlugin.CHECK_TASK_NAME }
      .configureEach {
        it.dependsOn("moduleCheck")
      }
  }

  private fun Project.registerTasks(
    name: String,
    findingFactory: FindingFactory<*>,
    includeAuto: Boolean,
    disableConfigCache: Boolean,
    doFirstAction: (() -> Unit)? = null
  ) {

    fun TaskProvider<*>.addDependencies() {
      configure { mcTask ->
        allprojects
          .filter { it.isMissingManifestFile() }
          .flatMap { it.tasks.withType(ManifestProcessorTask::class.java) }
          .forEach { mcTask.dependsOn(it) }

        allprojects
          .filter { it.generatesBuildConfig() }
          .flatMap { it.tasks.withType(GenerateBuildConfig::class.java) }
          .forEach { mcTask.dependsOn(it) }
      }
    }

    tasks.register(name, ModuleCheckTask::class.java, findingFactory, false, disableConfigCache)
      .also { if (doFirstAction != null) it.configure { it.doFirst { doFirstAction() } } }
      .addDependencies()
    if (includeAuto) {
      tasks.register(
        "${name}Auto",
        ModuleCheckTask::class.java,
        findingFactory,
        true,
        disableConfigCache
      )
        .also { if (doFirstAction != null) it.configure { it.doFirst { doFirstAction() } } }
        .addDependencies()
    }
  }
}
