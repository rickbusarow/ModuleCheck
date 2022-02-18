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
import modulecheck.parsing.gradle.SourceSet
import modulecheck.parsing.gradle.toSourceSetName
import modulecheck.utils.cast
import org.gradle.api.DefaultTask
import org.gradle.api.logging.LogLevel.WARN
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import javax.inject.Inject

open class ModuleCheckTask<T : Finding> @Inject constructor(
  private val findingFactory: FindingFactory<T>,
  private val autoCorrect: Boolean
) : DefaultTask() {

  init {
    group = "moduleCheck"
    description = if (findingFactory is SingleRuleFindingFactory<*>) {
      findingFactory.rule.description
    } else {
      "runs all enabled ModuleCheck rules"
    }
  }

  @get:Input
  val settings: ModuleCheckExtension = project.extensions.getByType()

  private val component
    get() = DaggerTaskComponent.factory()
      .create(project, settings, findingFactory)

  private val result by lazy {

    project.extensions.findByType<JavaPluginExtension>()
      ?.sourceSets
      .orEmpty()
      .map { it.name }
      .let {
        println("variant names -- $it")
      }

    SourceSet("dumb!".toSourceSetName())
  }
  // private val result by lazy {
  //   val projectProvider = component.gradleProjectProvider.create(project)
  //
  //   val runner = component.runnerFactory.create(projectProvider, autoCorrect)
  //
  //   val projects = projectProvider.getAll()
  //
  //   runner.run(projects)
  // }

  @TaskAction
  fun run() {

    logger.log(WARN, result.toString())

    try {

      // result.exceptionOrNull()
      //   ?.let { throw GradleException(it.message!!, it) }
    } finally {

      val dispatcherProvider = component.cast<DispatcherProviderComponent>()
        .dispatcherProvider

      dispatcherProvider.default.cancel()
      dispatcherProvider.io.cancel()
    }
  }
}
