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

package modulecheck.core.context

import kotlinx.coroutines.flow.toList
import modulecheck.finding.FindingName
import modulecheck.finding.UnusedKaptProcessorFinding
import modulecheck.model.dependency.ConfigurationName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.cache.SafeCache
import modulecheck.utils.coroutines.mapAsync
import modulecheck.utils.mapToSet

data class UnusedKaptProcessors(
  private val delegate: SafeCache<ConfigurationName, Set<UnusedKaptProcessorFinding>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<UnusedKaptProcessors>
    get() = Key

  suspend fun all(): List<UnusedKaptProcessorFinding> {
    return project
      .configurations
      .keys
      .filter { it.isKapt() }
      .mapAsync { configurationName -> get(configurationName) }
      .toList()
      .flatten()
      .distinct()
  }

  suspend fun get(configurationName: ConfigurationName): Set<UnusedKaptProcessorFinding> {

    return delegate.getOrPut(configurationName) {

      project.unusedDependencies()
        .get(configurationName)
        .mapToSet { unusedDependency ->
          UnusedKaptProcessorFinding(
            findingName = FindingName("unused-kapt-processor"),
            dependentProject = project,
            dependentPath = project.projectPath,
            buildFile = project.buildFile,
            oldDependency = unusedDependency.dependency,
            configurationName = unusedDependency.configurationName
          )
        }
    }
  }

  companion object Key : ProjectContext.Key<UnusedKaptProcessors> {
    override suspend operator fun invoke(project: McProject): UnusedKaptProcessors {

      return UnusedKaptProcessors(
        SafeCache(listOf(project.projectPath, UnusedKaptProcessors::class)),
        project
      )
    }
  }
}

suspend fun ProjectContext.unusedKaptProcessors(): UnusedKaptProcessors = get(UnusedKaptProcessors)
