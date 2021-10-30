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
import modulecheck.api.FindingFactory
import modulecheck.core.rule.ModuleCheckRuleFactory
import modulecheck.core.rule.MultiRuleFindingFactory
import modulecheck.core.rule.SingleRuleFindingFactory
import modulecheck.gradle.internal.isMissingManifestFile
import modulecheck.gradle.task.ModuleCheckTask
import org.gradle.api.Plugin
import org.gradle.api.Project
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

    rules.map { rule ->
      target.registerTask(
        name = rule.id,
        findingFactory = SingleRuleFindingFactory(rule),
        autoCorrect = false
      )
    }
    rules.map { rule ->
      target.registerTask(
        name = "${rule.id}Apply",
        findingFactory = SingleRuleFindingFactory(rule),
        autoCorrect = true
      )
    }

    target.registerTask(
      name = "moduleCheck",
      findingFactory = MultiRuleFindingFactory(settings, rules),
      autoCorrect = false
    )

    target.registerTask(
      name = "moduleCheckApply",
      findingFactory = MultiRuleFindingFactory(settings, rules),
      autoCorrect = true
    )
  }

  private fun Project.registerTask(
    name: String,
    findingFactory: FindingFactory<*>,
    autoCorrect: Boolean
  ) {
    tasks.register(name, ModuleCheckTask::class.java, findingFactory, autoCorrect)
      .configure {

        allprojects
          .filter { it.isMissingManifestFile() }
          .flatMap { it.tasks.withType(ManifestProcessorTask::class.java) }
          .forEach { dependsOn(it) }

        allprojects
          .flatMap { it.tasks.withType(GenerateBuildConfig::class.java) }
          .forEach { dependsOn(it) }
      }
  }
}
