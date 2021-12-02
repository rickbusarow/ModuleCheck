/*
 * Copyright (C) 2021 Rick Busarow
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
import modulecheck.api.finding.FindingFactory
import modulecheck.core.rule.DepthRule
import modulecheck.core.rule.ModuleCheckRuleFactory
import modulecheck.core.rule.MultiRuleFindingFactory
import modulecheck.core.rule.SingleRuleFindingFactory
import modulecheck.core.rule.SortDependenciesRule
import modulecheck.core.rule.SortPluginsRule
import modulecheck.gradle.internal.isMissingManifestFile
import modulecheck.gradle.task.ModuleCheckTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.configure

fun Project.moduleCheck(config: ModuleCheckExtension.() -> Unit) {
  extensions.configure(ModuleCheckExtension::class, config)
}

typealias GradleProject = Project

class ModuleCheckPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    val settings = target.extensions
      .create("moduleCheck", ModuleCheckExtension::class.java)

    val factory = ModuleCheckRuleFactory()

    val rules = factory.create(settings)

    target.registerTasks(
      name = "moduleCheckSortDependencies",
      findingFactory = SingleRuleFindingFactory(SortDependenciesRule(settings))
    )
    target.registerTasks(
      name = "moduleCheckSortPlugins",
      findingFactory = SingleRuleFindingFactory(SortPluginsRule(settings))
    )
    target.registerTasks(
      name = "moduleCheckDepths",
      findingFactory = SingleRuleFindingFactory(DepthRule())
    )
    target.registerTasks(
      name = "moduleCheckGraphs",
      findingFactory = SingleRuleFindingFactory(DepthRule())
    )
    target.registerTasks(
      name = "moduleCheck",
      findingFactory = MultiRuleFindingFactory(settings, rules)
    )
  }

  private fun Project.registerTasks(
    name: String,
    findingFactory: FindingFactory<*>
  ) {

    fun TaskProvider<*>.addDependencies() {
      configure {

        allprojects
          .filter { it.isMissingManifestFile() }
          .flatMap { it.tasks.withType(ManifestProcessorTask::class.java) }
          .forEach { dependsOn(it) }

        allprojects
          .flatMap { it.tasks.withType(GenerateBuildConfig::class.java) }
          .forEach { dependsOn(it) }
      }
    }

    tasks.register(name, ModuleCheckTask::class.java, findingFactory, false)
      .addDependencies()
    tasks.register("${name}Auto", ModuleCheckTask::class.java, findingFactory, true)
      .addDependencies()
  }
}
