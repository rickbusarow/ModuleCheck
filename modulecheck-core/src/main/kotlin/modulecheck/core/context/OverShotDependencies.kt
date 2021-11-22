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

package modulecheck.core.context

import modulecheck.core.OverShotDependencyFinding
import modulecheck.core.internal.uses
import modulecheck.parsing.ConfigurationName
import modulecheck.parsing.ConfiguredProjectDependency
import modulecheck.parsing.McProject
import modulecheck.parsing.ProjectContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class OverShotDependencies(
  internal val delegate: ConcurrentMap<ConfigurationName, Set<OverShotDependencyFinding>>
) : ConcurrentMap<ConfigurationName, Set<OverShotDependencyFinding>> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<OverShotDependencies>
    get() = Key

  companion object Key : ProjectContext.Key<OverShotDependencies> {
    override suspend operator fun invoke(project: McProject): OverShotDependencies {

      val used = project.unusedDependencies()
        .values
        .flatMap { allUnused ->
          allUnused
            .flatMap { unused ->

              val configSuffix = unused.configurationName
                .nameWithoutSourceSet()
                .takeIf { !it.equals(ConfigurationName.api.value, ignoreCase = true) }
                ?: ConfigurationName.implementation.value

              val allUsedByConfigName = project.sourceSets
                .keys
                .mapNotNull { sourceSetName ->

                  sourceSetName.configurationNames()
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
        }

      val grouped = used.map { (cpp, originalConfigurationName) ->

        OverShotDependencyFinding(
          dependentPath = project.path,
          buildFile = project.buildFile,
          dependencyProject = cpp.project,
          dependencyIdentifier = cpp.project.path,
          configurationName = cpp.configurationName,
          originalConfigurationName = originalConfigurationName,
          isTestFixture = cpp.isTestFixture
        )
      }
        .groupBy { it.configurationName }
        .mapValues { it.value.toSet() }

      return OverShotDependencies(ConcurrentHashMap(grouped))
    }
  }
}
