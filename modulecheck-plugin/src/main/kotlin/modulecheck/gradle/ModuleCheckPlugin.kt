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
import modulecheck.api.Finding
import modulecheck.core.rule.ModuleCheckRule
import modulecheck.core.rule.ModuleCheckRuleFactory
import modulecheck.gradle.internal.isMissingManifestFile
import modulecheck.gradle.task.ModuleCheckAllTask
import modulecheck.gradle.task.ModuleCheckTask
import modulecheck.parsing.McProject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Internal
import org.gradle.kotlin.dsl.configure
import javax.inject.Inject
import kotlin.reflect.KClass

fun Project.moduleCheck(config: ModuleCheckExtension.() -> Unit) {
  extensions.configure(ModuleCheckExtension::class, config)
}

typealias GradleProject = Project

class ModuleCheckPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    val settings = target.extensions
      .create("moduleCheck", ModuleCheckExtension::class.java)

    val factory = ModuleCheckRuleFactory()

    // AnvilFactoryRule is defined in this module, so it can't be statically registered like the others
    factory.register { AnvilFactoryRule(it) }

    val rules = factory.create(settings)

    rules.map { rule ->
      target.registerTask(
        name = "moduleCheck${rule.id}",
        type = DynamicModuleCheckTask::class,
        rules = rule
      )
    }

    target.registerTask(
      name = "moduleCheck",
      type = ModuleCheckAllTask::class,
      rules = rules
    )
  }

  private fun Project.registerTask(
    name: String,
    type: KClass<out Task>,
    rules: Any
  ) {
    tasks.register(name, type.java, rules)
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

abstract class DynamicModuleCheckTask<T : Finding> @Inject constructor(
  @Internal
  val rule: ModuleCheckRule<T>
) : ModuleCheckTask() {

  init {
    description = rule.description
  }

  override fun List<McProject>.getFindings(): List<T> {
    return flatMap { project ->
      rule.check(project)
    }
  }
}
