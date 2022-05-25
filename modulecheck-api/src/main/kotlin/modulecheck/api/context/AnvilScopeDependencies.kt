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

package modulecheck.api.context

import modulecheck.parsing.gradle.model.ConfiguredProjectDependency
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.project
import modulecheck.utils.cache.SafeCache

data class AnvilScopeDependencies(
  private val delegate: SafeCache<SourceSetName, List<ConfiguredProjectDependency>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<AnvilScopeDependencies>
    get() = Key

  suspend fun get(sourceSetName: SourceSetName): List<ConfiguredProjectDependency> {
    return delegate.getOrPut(sourceSetName) {
      val merged = project.anvilScopeMergesForSourceSetName(sourceSetName)

      // if the project/source set doesn't merge anything, skip looking for contributions
      if (merged.isEmpty()) return@getOrPut emptyList()

      project.classpathDependencies()
        .get(sourceSetName)
        .map { it.contributed }
        .distinct()
        .filter { cpd ->

          val contributed = cpd
            .project(project.projectCache)
            .anvilScopeContributionsForSourceSetName(cpd.configurationName.toSourceSetName())

          contributed.any { (scopeName, _) ->
            merged[scopeName]?.isNotEmpty() == true
          }
        }
    }
  }

  companion object Key : ProjectContext.Key<AnvilScopeDependencies> {

    override suspend operator fun invoke(project: McProject): AnvilScopeDependencies {
      return AnvilScopeDependencies(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.anvilScopeDependencies(): AnvilScopeDependencies =
  get(AnvilScopeDependencies)

suspend fun ProjectContext.anvilScopeDependenciesForSourceSetName(
  sourceSetName: SourceSetName
): List<ConfiguredProjectDependency> = anvilScopeDependencies().get(sourceSetName)
