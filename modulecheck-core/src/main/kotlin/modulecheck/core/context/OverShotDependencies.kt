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
    override operator fun invoke(project: McProject): OverShotDependencies {

      val used = project.unusedDependencies
        .values
        .flatMap { allUnused ->
          allUnused
            .flatMap { unused ->

              val configSuffix = unused.configurationName
                .nameWithoutSourceSet()
                .takeIf { !it.equals(ConfigurationName.api.value, ignoreCase = true) }
                ?: ConfigurationName.implementation.value

              val all = project.configurations
                .values
                .asSequence()
                .filterNot { it.name.nameWithoutSourceSet().isBlank() }
                .sortedByDescending {
                  it.name.nameWithoutSourceSet()
                    .equals(configSuffix, ignoreCase = true)
                }
                .mapNotNull { dependentConfig ->

                  ConfiguredProjectDependency(
                    configurationName = dependentConfig.name,
                    project = unused.dependencyProject,
                    isTestFixture = unused.cpd().isTestFixture
                  )
                    .takeIf { project.uses(it) }
                }
                .distinctBy { it.configurationName.toSourceSetName() }
                .groupBy { it.configurationName }

              val allConfigs = all.values
                .flatMap { cpds ->
                  cpds.map { project.configurations.getValue(it.configurationName) }
                }
                .distinct()

              val top = allConfigs.filter { cfg ->
                cfg.inherited.none { it in allConfigs }
              }

              top.flatMap { all.getValue(it.name) }
                .filter { project.projectDependencies[it.configurationName]?.contains(it) != true }
                .toSet()
            }
        }

      val grouped = used.map { cpp ->

        OverShotDependencyFinding(
          dependentPath = project.path,
          buildFile = project.buildFile,
          dependencyProject = cpp.project,
          dependencyIdentifier = cpp.project.path,
          configurationName = cpp.configurationName
        )
      }
        .groupBy { it.configurationName }
        .mapValues { it.value.toSet() }

      return OverShotDependencies(ConcurrentHashMap(grouped))
    }
  }
}
