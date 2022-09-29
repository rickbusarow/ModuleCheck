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

import kotlinx.coroutines.flow.toList
import modulecheck.api.context.classpathDependencies
import modulecheck.finding.RedundantDependency
import modulecheck.model.dependency.javaConfigurationNames
import modulecheck.model.sourceset.SourceSetName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.cache.SafeCache
import modulecheck.utils.coroutines.mapAsync

data class RedundantDependencies(
  private val delegate: SafeCache<SourceSetName, List<RedundantDependency>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<RedundantDependencies>
    get() = Key

  suspend fun all(): List<RedundantDependency> {
    return project.sourceSets
      .keys
      .mapAsync { get(it) }
      .toList()
      .flatten()
  }

  suspend fun get(sourceSetName: SourceSetName): List<RedundantDependency> {

    return delegate.getOrPut(sourceSetName) {

      val allDirect = sourceSetName.javaConfigurationNames()
        .flatMap { project.projectDependencies[it].orEmpty() }
        .toSet()

      val inheritedDependencyProjects = project
        .classpathDependencies()
        .get(sourceSetName)
        .groupBy { it.contributed.projectPath }

      allDirect
        .mapNotNull { direct ->

          val fromCpd = inheritedDependencyProjects[direct.projectPath]
            ?.map { it.source }
            ?: return@mapNotNull null

          RedundantDependency(
            dependentProject = project,
            dependency = direct,
            configurationName = direct.configurationName,
            from = fromCpd
          )
        }
    }
  }

  companion object Key : ProjectContext.Key<RedundantDependencies> {
    override suspend operator fun invoke(project: McProject): RedundantDependencies {

      return RedundantDependencies(
        SafeCache(listOf(project.projectPath, RedundantDependencies::class)),
        project
      )
    }
  }
}

suspend fun ProjectContext.redundantDependencies(): RedundantDependencies =
  get(RedundantDependencies)
