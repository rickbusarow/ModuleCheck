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
import modulecheck.api.finding.Finding
import modulecheck.api.finding.FindingFactory
import modulecheck.core.rule.SingleRuleFindingFactory
import modulecheck.dagger.DispatcherProviderComponent
import modulecheck.gradle.ModuleCheckExtension
import modulecheck.utils.cast
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

open class ModuleCheckTask<T : Finding> @Inject constructor(
  private val findingFactory: FindingFactory<T>,
  private val autoCorrect: Boolean,
  disableConfigCache: Boolean
) : DefaultTask() {

  init {
    group = "moduleCheck"
    description = when {
      findingFactory is SingleRuleFindingFactory<*> -> findingFactory.rule.description
      autoCorrect -> "runs all enabled ModuleCheck rules with auto-correct"
      else -> "runs all enabled ModuleCheck rules"
    }

    if (disableConfigCache) {
      // If the runtime Gradle distro is 7.4+, disable configuration caching.
      // This function was introduced in 7.4.
      @Suppress("LeakingThis")
      notCompatibleWithConfigurationCache("Not supported yet")
    }
  }

  @get:Input
  val settings: ModuleCheckExtension = project.extensions
    .getByType(ModuleCheckExtension::class.java)

  @TaskAction
  fun run() {

    val component = DaggerTaskComponent.factory()
      .create(
        project = project,
        moduleCheckSettings = settings,
        findingFactory = findingFactory,
        projectRoot = { project.rootDir }
      )

    try {

      val projectProvider = component.gradleProjectProvider.create(project)
      val runner = component.runnerFactory.create(projectProvider, autoCorrect)

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
}
