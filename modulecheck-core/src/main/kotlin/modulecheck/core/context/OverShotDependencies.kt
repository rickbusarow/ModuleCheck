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
import modulecheck.project.toSourceSetDependency
import modulecheck.utils.SafeCache
import modulecheck.utils.mapToSet
import modulecheck.utils.unsafeLazy

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
          val unusedSsd =
            unusedCpd.toSourceSetDependency(unused.configurationName.toSourceSetName())
          val unusedSourceSetName = unused.configurationName.toSourceSetName()

          val allUsedByConfigName = unusedSourceSetName
            .withDownStream(project)
            .mapNotNull { sourceSetName ->

              val existingDependencies by unsafeLazy {
                project.projectDependencies[sourceSetName]
                  .mapToSet { it.toSourceSetDependency(sourceSetName) }
              }

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
                    project = unused.dependency.project,
                    isTestFixture = isTestFixture
                  )
                }
                .filterNot {

                  val asSsd = it.toSourceSetDependency(sourceSetName)

                  asSsd == unusedSsd || existingDependencies.contains(asSsd)
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
            .map { it to unused.oldDependency }
            .toSet()
        }
        .map { (overshot, original) ->

          val newCpd = overshot.asApiOrImplementation(project)

          OverShotDependencyFinding(
            dependentProject = project,
            newDependency = newCpd,
            oldDependency = original,
            configurationName = newCpd.configurationName
          )
        }
        .sortedBy { it.dependency.project }
        .distinctBy { it.dependency.project }
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
