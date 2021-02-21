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

import modulecheck.api.Finding
import modulecheck.api.Project2
import modulecheck.api.settings.ModuleCheckExtension
import modulecheck.core.rule.ModuleCheckRule
import modulecheck.core.rule.ModuleCheckRuleFactory
import modulecheck.gradle.task.ModuleCheckAllTask
import modulecheck.gradle.task.ModuleCheckTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import javax.inject.Inject

fun Project.moduleCheck(config: ModuleCheckExtension.() -> Unit) {
  extensions.configure(ModuleCheckExtension::class, config)
}

class ModuleCheckPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    val settings = target.extensions.create("moduleCheck", ModuleCheckExtension::class.java)

    val factory = ModuleCheckRuleFactory()

    val rules = factory.create(settings)

    rules
      .onEach { rule ->
        target.tasks.register("moduleCheck${rule.id}", DynamicModuleCheckTask::class, rule)
      }

    target.tasks.register("moduleCheck", ModuleCheckAllTask::class.java, rules)
  }
}

abstract class DynamicModuleCheckTask<T : Finding> @Inject constructor(
  val rule: ModuleCheckRule<T>
) : ModuleCheckTask() {

  init {
    description = rule.description
  }

  override fun List<Project2>.getFindings(): List<T> {
    return flatMap { project ->
      rule.check(project)
    }
  }
}
