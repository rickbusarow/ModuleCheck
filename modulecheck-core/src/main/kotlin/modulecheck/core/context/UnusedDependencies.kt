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

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import modulecheck.api.context.uses
import modulecheck.finding.UnusedDependency
import modulecheck.model.dependency.ProjectDependency
import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.cache.SafeCache
import modulecheck.utils.coroutines.filterAsync
import modulecheck.utils.coroutines.mapAsync

data class UnusedDependencies(
  private val delegate: SafeCache<ConfigurationName, Set<UnusedDependency>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<UnusedDependencies>
    get() = Key

  suspend fun all(): List<UnusedDependency> {
    return project.sourceSets
      .flatMap { it.key.javaConfigurationNames() }
      .mapAsync { configurationName -> get(configurationName) }
      .toList()
      .flatten()
      .distinct()
  }

  suspend fun get(configurationName: ConfigurationName): Set<UnusedDependency> {

    return delegate.getOrPut(configurationName) {
      val external = project.externalDependencies[configurationName].orEmpty()
      val internal = project.projectDependencies[configurationName].orEmpty()

      val dependencies = external + internal

      dependencies
        .asSequence()
        // test configurations have the main source project as a dependency.
        // without this filter, every project will report itself as unused.
        .filterNot { cd -> (cd as? ProjectDependency)?.path == project.path }
        .filterAsync { dependency ->
          !project.uses(dependency)
        }
        .map { dependency ->
          UnusedDependency(
            dependentProject = project,
            dependency = dependency,
            dependencyIdentifier = dependency.identifier,
            configurationName = dependency.configurationName
          )
        }
        .toSet()
    }
  }

  companion object Key : ProjectContext.Key<UnusedDependencies> {
    override suspend operator fun invoke(project: McProject): UnusedDependencies {

      return UnusedDependencies(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.unusedDependencies(): UnusedDependencies = get(UnusedDependencies)
