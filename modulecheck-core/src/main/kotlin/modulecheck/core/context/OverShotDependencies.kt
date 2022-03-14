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

          val unusedCpd = unused.cpd()
          val unusedSourceSetName = unused.configurationName.toSourceSetName()

          val allUsedByConfigName = unusedSourceSetName
            .withDownStream(project)
            .mapNotNull { sourceSetName ->

              val configName = sourceSetName.implementationConfig()

              val seed = when {

                // If the SourceSet is the same as the original unused dependency, then the original
                // must be testFixtures.  Only check the non-testFixtures version.
                sourceSetName == unusedSourceSetName -> sequenceOf(configName to false)

                // If the unused was testFixture, that testFixture source may be used downstream.
                // So, check testFixture first, then the normal/main source.
                unusedCpd.isTestFixture -> sequenceOf(configName to true, configName to false)
                // If the unused wasn't a testFixture, then just check normal/main source.
                else -> sequenceOf(configName to false)
              }

              seed
                .map { (configName, isTestFixture) ->
                  ConfiguredProjectDependency(
                    configurationName = configName,
                    project = unused.dependencyProject,
                    isTestFixture = isTestFixture
                  )
                }
                .filterNot {
                  it.isTestFixture == unused.cpd().isTestFixture &&
                    it.configurationName.toSourceSetName() == unused.cpd()
                    .configurationName
                    .toSourceSetName()
                }
                .firstNotNullOfOrNull { cpd ->

                  cpd.takeIf { project.uses(it) }
                }
            }
            .groupBy { it.configurationName }

          val allConfigs = allUsedByConfigName.keys
            .mapNotNull { configurationName ->
              project.configurations[configurationName]
            }
            .distinct()

          // Remove redundant configs
          // For instance, don't add a `testImplementation` declaration if `implementation` is
          // already being added.
          val trimmedConfigs = allConfigs.filter { cfg ->
            cfg.upstream.none { it in allConfigs }
          }

          trimmedConfigs.flatMap { allUsedByConfigName.getValue(it.name) }
            .filter { project.projectDependencies[it.configurationName]?.contains(it) != true }
            .map { it to unused.configurationName }
            .toSet()
        }
        .map { (overshot, originalConfigurationName) ->

          val newCpd = overshot.asApiOrImplementation(project)

          OverShotDependencyFinding(
            dependentProject = project,
            newDependency = newCpd,
            oldDependency = overshot.copy(configurationName = originalConfigurationName),
            configurationName = newCpd.configurationName
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
