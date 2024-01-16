/*
 * Copyright (C) 2021-2024 Rick Busarow
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

/**
 * Represents a data class that contains all unused Kapt processors in a project.
 *
 * @param delegate a [SafeCache] instance which manages a cache of
 *   configuration names to the associated unused Kapt processors.
 * @param project the [McProject] for which the unused Kapt processors are retrieved.
 */
data class UnusedKaptProcessors(
  private val delegate: SafeCache<ConfigurationName, Set<UnusedKaptProcessorFinding>>,
  private val project: McProject
) : ProjectContext.Element {

  /** Key used to identify this [UnusedKaptProcessors] instance within [ProjectContext]. */
  override val key: ProjectContext.Key<UnusedKaptProcessors>
    get() = Key

  /**
   * Retrieves all unused Kapt processors in the project.
   *
   * @return A list of [UnusedKaptProcessorFinding]s
   *   representing all unused Kapt processors in the project.
   */
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

  /**
   * Retrieves the unused Kapt processors for a given configuration.
   *
   * @param configurationName The name of the configuration
   *   for which to retrieve the unused Kapt processors.
   * @return A set of [UnusedKaptProcessorFinding]s representing
   *   the unused Kapt processors in the requested configuration.
   */
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

  /** Key object used to identify [UnusedKaptProcessors] within [ProjectContext]. */
  companion object Key : ProjectContext.Key<UnusedKaptProcessors> {
    /**
     * Creates a new instance of [UnusedKaptProcessors] for a given project.
     *
     * @param project The project for which to create the [UnusedKaptProcessors] instance.
     * @return A new instance of [UnusedKaptProcessors].
     */
    override suspend operator fun invoke(project: McProject): UnusedKaptProcessors {

      return UnusedKaptProcessors(
        SafeCache(listOf(project.projectPath, UnusedKaptProcessors::class)),
        project
      )
    }
  }
}

/**
 * Extension function on [ProjectContext] to get [UnusedKaptProcessors] instance.
 *
 * @return The [UnusedKaptProcessors] instance for this context.
 */
suspend fun ProjectContext.unusedKaptProcessors(): UnusedKaptProcessors = get(UnusedKaptProcessors)
