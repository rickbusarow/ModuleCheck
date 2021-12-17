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

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import modulecheck.api.context.anvilScopeDependenciesForSourceSetName
import modulecheck.core.UnusedDependency
import modulecheck.core.internal.uses
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.SafeCache
import modulecheck.utils.filterAsync
import modulecheck.utils.lazyDeferred
import modulecheck.utils.mapAsync

data class UnusedDependencies(
  private val delegate: SafeCache<ConfigurationName, Set<UnusedDependency>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<UnusedDependencies>
    get() = Key

  suspend fun all(): List<UnusedDependency> {
    return project.sourceSets
      .flatMap { it.key.configurationNames() }
      .mapAsync { configurationName -> get(configurationName) }
      .toList()
      .flatten()
      .distinct()
  }

  suspend fun get(configurationName: ConfigurationName): Set<UnusedDependency> {

    return delegate.getOrPut(configurationName) {
      val deps = project.projectDependencies[configurationName] ?: return@getOrPut emptySet()

      val neededForScopes = lazyDeferred {
        project.anvilScopeDependenciesForSourceSetName(configurationName.toSourceSetName())
          .map { it.project }
          .toSet()
      }

      deps.filterNot { cpd ->
        // test configurations have the main source project as a dependency.
        // without this, every project will report itself as unused.
        cpd.project.path == project.path
      }
        .asFlow()
        .filterAsync { cpd ->
          !project.uses(cpd) && !neededForScopes.await().contains(cpd.project)
        }
        .map { cpd ->
          UnusedDependency(
            dependentProject = project,
            oldDependency = cpd,
            dependencyIdentifier = cpd.project.path,
            configurationName = cpd.configurationName
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
