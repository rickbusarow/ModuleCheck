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

package modulecheck.core.context

import modulecheck.core.OverShotDependencyFinding
import modulecheck.core.internal.uses
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.SafeCache

data class OverShotDependencies(
  private val delegate: SafeCache<ConfigurationName, List<OverShotDependencyFinding>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<OverShotDependencies>
    get() = Key

  suspend fun get(configurationName: ConfigurationName): List<OverShotDependencyFinding> {
    return delegate.getOrPut(configurationName) {

      project.unusedDependencies()
        .get(configurationName)
        .flatMap { unused ->

          val configSuffix = unused.configurationName
            .nameWithoutSourceSet()
            .takeIf { !it.equals(ConfigurationName.api.value, ignoreCase = true) }
            ?: ConfigurationName.implementation.value

          val allUsedByConfigName = project.sourceSets
            .keys
            .mapNotNull { sourceSetName ->

              sourceSetName.javaConfigurationNames()
                .filterNot { it == unused.configurationName }
                // check the same config as the unused configuration first.
                // for instance, if `api` is unused, check `debugApi`, `testApi`, etc.
                .sortedByDescending {
                  it.nameWithoutSourceSet().equals(configSuffix, ignoreCase = true)
                }
                .firstNotNullOfOrNull { configName ->
                  ConfiguredProjectDependency(
                    configurationName = configName,
                    project = unused.dependencyProject,
                    isTestFixture = unused.cpd().isTestFixture
                  )
                    .takeIf { project.uses(it) }
                }
            }
            .groupBy { it.configurationName }

          val allConfigs = allUsedByConfigName.values
            .flatMap { cpds ->
              cpds.map { project.configurations.getValue(it.configurationName) }
            }
            .distinct()

          // Remove redundant configs
          // For instance, don't add a `testImplementation` declaration if `implementation` is
          // already being added.
          val trimmedConfigs = allConfigs.filter { cfg ->
            cfg.inherited.none { it in allConfigs }
          }

          trimmedConfigs.flatMap { allUsedByConfigName.getValue(it.name) }
            .filter { project.projectDependencies[it.configurationName]?.contains(it) != true }
            .map { it to unused.configurationName }
            .toSet()
        }
        .map { (cpp, originalConfigurationName) ->

          OverShotDependencyFinding(
            dependentProject = project,
            newDependency = cpp,
            oldDependency = cpp.copy(configurationName = originalConfigurationName),
            configurationName = cpp.configurationName
          )
        }
        .sortedBy { it.dependencyProject }
        .distinctBy { it.dependencyProject }
    }
  }

  suspend fun all(): List<OverShotDependencyFinding> {
    return project.configurations.keys.flatMap { get(it) }
  }

  companion object Key : ProjectContext.Key<OverShotDependencies> {
    override suspend operator fun invoke(project: McProject): OverShotDependencies {

      return OverShotDependencies(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.overshotDependencies(): OverShotDependencies = get(OverShotDependencies)
