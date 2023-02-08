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

import modulecheck.api.context.maybeAsApi
import modulecheck.api.context.uses
import modulecheck.finding.OverShotDependency
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.ConfiguredDependency.Companion.copy
import modulecheck.model.dependency.toSourceSetDependency
import modulecheck.model.dependency.withDownStream
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.cache.SafeCache
import modulecheck.utils.lazy.unsafeLazy
import modulecheck.utils.mapToSet

data class OverShotDependencies(
  private val delegate: SafeCache<ConfigurationName, List<OverShotDependency>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<OverShotDependencies>
    get() = Key

  suspend fun all(): List<OverShotDependency> {
    return project.configurations.keys.flatMap { get(it) }
  }

  suspend fun get(configurationName: ConfigurationName): List<OverShotDependency> {
    return delegate.getOrPut(configurationName) {

      project.unusedDependencies()
        .get(configurationName)
        .flatMap { unused ->

          val unusedCpd = unused.dependency
          val unusedSsd = unusedCpd
            .toSourceSetDependency(unused.configurationName.toSourceSetName())
          val unusedSourceSetName = unused.configurationName.toSourceSetName()

          val allUsedByConfigName = unusedSourceSetName
            .withDownStream(project)
            .mapNotNull { sourceSetName ->

              val existingDependencies by unsafeLazy {
                project.projectDependencies[sourceSetName]
                  .mapToSet { it.toSourceSetDependency(sourceSetName) }
              }

              val configName = unused.configurationName.switchSourceSet(sourceSetName)

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
                  unused.dependency.copy(
                    configurationName = configName,
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
            .map { it to unused.dependency }
            .toSet()
        }
        .map { (overshot, original) ->

          val newDependency = overshot.maybeAsApi(project)

          OverShotDependency(
            dependentProject = project,
            newDependency = newDependency,
            oldDependency = original
          )
        }
        .sortedBy { it.newDependency.identifier.name }
        // Only report each new path/configuration pair once.  So we can add multiple dependencies
        // for the `testImplementation` config, or multiple configurations of the `:lib1` project,
        // but we'll only add `testImplementation(project(":lib1"))` once.
        .distinctBy { it.newDependency.identifier to it.newDependency.configurationName }
    }
  }

  companion object Key : ProjectContext.Key<OverShotDependencies> {
    override suspend operator fun invoke(project: McProject): OverShotDependencies {

      return OverShotDependencies(
        SafeCache(listOf(project.projectPath, OverShotDependencies::class)),
        project
      )
    }
  }
}

suspend fun ProjectContext.overshotDependencies(): OverShotDependencies = get(OverShotDependencies)
