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

package modulecheck.gradle.task

import kotlinx.coroutines.cancel
import modulecheck.finding.FindingName
import modulecheck.gradle.ModuleCheckExtension
import modulecheck.model.sourceset.HasSourceSetName
import modulecheck.rule.RuleFilter
import modulecheck.utils.cast
import modulecheck.utils.coroutines.impl.DispatcherProviderComponent
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/** Represents a task that checks a specific source set for module issues. */
abstract class ModuleCheckSourceSetTask : AbstractModuleCheckTask(), HasSourceSetName

/** Represents a task that checks for module issues. */
abstract class AbstractModuleCheckTask : DefaultTask() {

  init {
    group = "ModuleCheck"
  }
}

/** Represents a task that checks for module issues according to a specific rule. */
abstract class AbstractModuleCheckRuleTask(
  @Internal
  val workerExecutor: WorkerExecutor,
  objectFactory: ObjectFactory
) : AbstractModuleCheckTask() {

  @get:Input
  val autoCorrect: Property<Boolean> = objectFactory.property(Boolean::class.java)

  /** Provides the rule filter for the task. */
  protected abstract fun ruleFilter(): RuleFilter

  @get:Input
  val settings: ModuleCheckExtension = project.extensions
    .getByType(ModuleCheckExtension::class.java)

  @get:Internal
  protected val component by lazy {
    DaggerTaskComponent.factory()
      .create(
        rootProject = project,
        moduleCheckSettings = settings,
        ruleFilter = ruleFilter(),
        projectRoot = { project.rootDir },
        workerExecutor = workerExecutor
      )
  }

  /** Executes the task. */
  @TaskAction
  fun run() {
    try {

      val projectProvider = component.projectProvider
      val runner = component.runnerFactory.create(autoCorrect.get())

      val projects = projectProvider.getAll()

      val result = runner.run(projects)

      result.exceptionOrNull()
        ?.let { throw GradleException(it.message!!, it) }
    } finally {

      val dispatcherProvider = component.cast<DispatcherProviderComponent>()
        .dispatcherProvider

      dispatcherProvider.default.cancel()
      dispatcherProvider.io.cancel()
    }
  }

  /**
   * Disables configuration caching if the given condition is true.
   *
   * @param disableConfigCache The condition to disable configuration caching.
   */
  protected fun maybeDisableConfigurationCaching(disableConfigCache: Boolean) {
    if (disableConfigCache) {
      // If the runtime Gradle distro is 7.4+, disable configuration caching.
      // This function was introduced in 7.4.
      notCompatibleWithConfigurationCache("Not supported yet")
    }
  }
}

/** Represents a task that checks for module issues according to multiple rules. */
open class MultiRuleModuleCheckTask @Inject constructor(
  workerExecutor: WorkerExecutor,
  objectFactory: ObjectFactory
) : AbstractModuleCheckRuleTask(workerExecutor, objectFactory) {

  /**
   * Configures the task with the given parameters.
   *
   * @param autoCorrect Determines whether the task should auto-correct issues.
   * @param disableConfigCache The condition to disable configuration caching.
   */
  internal fun configure(autoCorrect: Boolean, disableConfigCache: Boolean) {
    this.autoCorrect.set(autoCorrect)

    description = if (autoCorrect) {
      "runs all enabled ModuleCheck rules with auto-correct"
    } else {
      "runs all enabled ModuleCheck rules"
    }

    maybeDisableConfigurationCaching(disableConfigCache)
  }

  /** @return The default rule filter. */
  override fun ruleFilter() = RuleFilter.DEFAULT
}

/** Represents a task that checks for module issues according to a single rule. */
open class SingleRuleModuleCheckTask @Inject constructor(
  workerExecutor: WorkerExecutor,
  objectFactory: ObjectFactory
) : AbstractModuleCheckRuleTask(workerExecutor, objectFactory) {
/** */
  @get:Input
  val findingName: Property<FindingName> = objectFactory.property(FindingName::class.java)

  /**
   * Configures the task with the given parameters.
   *
   * @param findingName The name of the finding that the rule checks for.
   * @param autoCorrect Determines whether the task should auto-correct issues.
   * @param disableConfigCache The condition to disable configuration caching.
   */
  internal fun configure(
    findingName: FindingName,
    autoCorrect: Boolean,
    disableConfigCache: Boolean
  ) {
    this.autoCorrect.set(autoCorrect)
    this.findingName.set(findingName)

    maybeDisableConfigurationCaching(disableConfigCache)

    description = if (autoCorrect) {
      "runs the ${findingName.id} ModuleCheck rule with auto-correct"
    } else {
      "runs the ${findingName.id} ModuleCheck rule"
    }
  }

  /** @return A rule filter that only includes the rule with the same name as the finding. */
  override fun ruleFilter() = RuleFilter { rule, _ ->
    rule.name == findingName.get()
  }
}
