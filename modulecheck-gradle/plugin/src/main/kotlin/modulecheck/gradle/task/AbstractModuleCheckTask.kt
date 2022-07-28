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
import modulecheck.parsing.gradle.model.GradleConfiguration
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

abstract class AbstractModuleCheckTask : DefaultTask() {

  init {
    group = "moduleCheck"
  }
}

open class ModuleCheckDependencyResolutionTask : AbstractModuleCheckTask() {
  init {
    description = "Resolves all external dependencies"
  }

  @TaskAction
  fun butt() {
    dependsOn.filterIsInstance<GradleConfiguration>()
      .forEach { it.resolve() }

    println("###################################### config files")
    project.configurations.getByName("moduleCheckDebugAggregateDependencies")
      .allDependencies
      // .flatMap {
      //   when (it) {
      //     is DefaultSelfResolvingDependency -> it.resolve()
      //     is DefaultExternalModuleDependency -> it.artifacts
      //     else -> emptyList()
      //   }
      // }
      // .filterIsInstance<DefaultSelfResolvingDependency>()
      // .flatMap { it.resolve() }
      .joinToString("\n")
      .also(::println)

    // println("###################################### depends on")
    // dependsOn.joinToString("\n")
    //   .also(::println)
    //
    // println("###################################### input files")
    // inputs.files.joinToString("\n")
    //   .also(::println)
    //
    // dependsOn.filterIsInstance<GradleConfiguration>()
    //   .forEach {
    //
    //     println("###################################### files  ${it.name}")
    //     it.dependencies.joinToString("\n").also(::println)
    //   }
  }
}

abstract class AbstractModuleCheckRuleTask(
  @Internal
  val workerExecutor: WorkerExecutor,
  objectFactory: ObjectFactory
) : AbstractModuleCheckTask() {

  @get:Input
  val autoCorrect: Property<Boolean> = objectFactory.property(Boolean::class.java)

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

  protected fun maybeDisableConfigurationCaching(disableConfigCache: Boolean) {
    if (disableConfigCache) {
      // If the runtime Gradle distro is 7.4+, disable configuration caching.
      // This function was introduced in 7.4.
      notCompatibleWithConfigurationCache("Not supported yet")
    }
  }
}

open class MultiRuleModuleCheckTask @Inject constructor(
  workerExecutor: WorkerExecutor,
  objectFactory: ObjectFactory
) : AbstractModuleCheckRuleTask(workerExecutor, objectFactory) {

  internal fun configure(autoCorrect: Boolean, disableConfigCache: Boolean) {
    this.autoCorrect.set(autoCorrect)

    description = if (autoCorrect) {
      "runs all enabled ModuleCheck rules with auto-correct"
    } else {
      "runs all enabled ModuleCheck rules"
    }

    maybeDisableConfigurationCaching(disableConfigCache)
  }

  override fun ruleFilter() = RuleFilter.DEFAULT
}

open class SingleRuleModuleCheckTask @Inject constructor(
  workerExecutor: WorkerExecutor,
  objectFactory: ObjectFactory
) : AbstractModuleCheckRuleTask(workerExecutor, objectFactory) {

  @get:Input
  val findingName: Property<FindingName> = objectFactory.property(FindingName::class.java)

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

  override fun ruleFilter() = RuleFilter { rule, _ ->
    rule.name == findingName.get()
  }
}
