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

package modulecheck.gradle.task

import kotlinx.coroutines.cancel
import modulecheck.finding.FindingName
import modulecheck.gradle.ModuleCheckExtension
import modulecheck.rule.RuleFilter
import modulecheck.utils.cast
import modulecheck.utils.coroutines.impl.DispatcherProviderComponent
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class AbstractModuleCheckTask(
  private val autoCorrect: Boolean,
  disableConfigCache: Boolean
) : DefaultTask() {

  init {
    group = "moduleCheck"

    if (disableConfigCache) {
      // If the runtime Gradle distro is 7.4+, disable configuration caching.
      // This function was introduced in 7.4.
      @Suppress("LeakingThis")
      notCompatibleWithConfigurationCache("Not supported yet")
    }
  }

  protected abstract fun ruleFilter(): RuleFilter

  @get:Input
  val settings: ModuleCheckExtension = project.extensions
    .getByType(ModuleCheckExtension::class.java)

  @get:Internal
  protected val component: TaskComponent by lazy {
    DaggerTaskComponent.factory()
      .create(
        rootProject = project,
        moduleCheckSettings = settings,
        ruleFilter = ruleFilter(),
        projectRoot = { project.rootDir }
      )
  }

  @TaskAction
  fun run() {
    try {

      val projectProvider = component.projectProvider
      val runner = component.runnerFactory.create(autoCorrect)

      val projects = projectProvider.getAll()

      val result = runner.run(projects)

      result.exceptionOrNull()
        ?.let {
          @Suppress("UnsafeCallOnNullableType")
          throw GradleException(it.message!!, it)
        }
    } finally {

      val dispatcherProvider = component.cast<DispatcherProviderComponent>()
        .dispatcherProvider

      dispatcherProvider.default.cancel()
      dispatcherProvider.io.cancel()
    }
  }
}

open class MultiRuleModuleCheckTask @Inject constructor(
  autoCorrect: Boolean,
  disableConfigCache: Boolean
) : AbstractModuleCheckTask(autoCorrect, disableConfigCache) {

  init {
    description = if (autoCorrect) {
      "runs all enabled ModuleCheck rules with auto-correct"
    } else {
      "runs all enabled ModuleCheck rules"
    }
  }

  override fun ruleFilter(): RuleFilter = RuleFilter.DEFAULT
}

open class SingleRuleModuleCheckTask @Inject constructor(
  private val findingName: FindingName,
  autoCorrect: Boolean,
  disableConfigCache: Boolean
) : AbstractModuleCheckTask(autoCorrect, disableConfigCache) {

  init {
    description = if (autoCorrect) {
      "runs the ${findingName.id} ModuleCheck rule with auto-correct"
    } else {
      "runs the ${findingName.id} ModuleCheck rule"
    }
  }

  override fun ruleFilter(): RuleFilter = RuleFilter { rule, _ ->
    rule.name == findingName
  }
}
