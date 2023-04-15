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

import com.squareup.anvil.annotations.MergeComponent
import dagger.BindsInstance
import dagger.Component
import modulecheck.config.ModuleCheckSettings
import modulecheck.dagger.RootGradleProject
import modulecheck.dagger.SingleIn
import modulecheck.dagger.TaskScope
import modulecheck.gradle.internal.GradleProjectProvider
import modulecheck.parsing.gradle.model.GradleProject
import modulecheck.project.ProjectRoot
import modulecheck.rule.RuleFilter
import modulecheck.rule.RulesComponent
import modulecheck.runtime.RunnerComponent
import org.gradle.workers.WorkerExecutor

@SingleIn(TaskScope::class)
@MergeComponent(TaskScope::class)
interface TaskComponent : RunnerComponent, RulesComponent {

  val projectProvider: GradleProjectProvider

  @Component.Factory
  interface Factory {
    /**
     * @param rootProject the root (`:`) rootProject
     * @param moduleCheckSettings settings...
     * @param ruleFilter this lets the tasks define which rule(s) they're going to apply
     * @param projectRoot the root directory for the rootProject.
     *   This is the same as calling `rootProject.rootDir`.
     * @param workerExecutor the only way into Gradle's managed thread ecosystem
     * @since 0.12.0
     */
    fun create(
      @RootGradleProject
      @BindsInstance
      rootProject: GradleProject,
      @BindsInstance
      moduleCheckSettings: ModuleCheckSettings,
      @BindsInstance
      ruleFilter: RuleFilter,
      @BindsInstance
      projectRoot: ProjectRoot,
      @BindsInstance
      workerExecutor: WorkerExecutor
    ): TaskComponent
  }
}
