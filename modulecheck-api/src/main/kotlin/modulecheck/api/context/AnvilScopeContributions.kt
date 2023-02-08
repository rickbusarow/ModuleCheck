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

package modulecheck.api.context

import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.source.AnvilScopeName
import modulecheck.parsing.source.QualifiedDeclaredName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.cache.SafeCache

data class AnvilScopeContributions(
  private val delegate: SafeCache<SourceSetName, Map<AnvilScopeName, Set<QualifiedDeclaredName>>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<AnvilScopeContributions>
    get() = Key

  /**
   * @return all contributed interfaces for this [sourceSetName], grouped by the [AnvilScopeName]
   *   for which they're contributed
   * @since 0.12.0
   */
  suspend fun get(sourceSetName: SourceSetName): Map<AnvilScopeName, Set<QualifiedDeclaredName>> {
    return delegate.getOrPut(sourceSetName) {
      project.anvilGraph().get(sourceSetName)
        .mapValues { (_, declarations) ->
          declarations.contributions
        }
        .filter { it.value.isNotEmpty() }
    }
  }

  companion object Key : ProjectContext.Key<AnvilScopeContributions> {

    override suspend operator fun invoke(project: McProject): AnvilScopeContributions {
      return AnvilScopeContributions(
        SafeCache(listOf(project.projectPath, AnvilScopeContributions::class)),
        project
      )
    }
  }
}

suspend fun ProjectContext.anvilScopeContributions(): AnvilScopeContributions =
  get(AnvilScopeContributions)

/**
 * @return all contributed interfaces for this [sourceSetName], grouped by the [AnvilScopeName] for
 *   which they're contributed
 * @since 0.12.0
 */
suspend fun ProjectContext.anvilScopeContributionsForSourceSetName(
  sourceSetName: SourceSetName
): Map<AnvilScopeName, Set<QualifiedDeclaredName>> = anvilScopeContributions().get(sourceSetName)
