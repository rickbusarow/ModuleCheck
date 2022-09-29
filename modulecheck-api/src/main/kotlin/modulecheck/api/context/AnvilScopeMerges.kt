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

import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.source.AnvilScopeName
import modulecheck.parsing.source.QualifiedDeclaredName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.cache.SafeCache

data class AnvilScopeMerges(
  private val delegate: SafeCache<SourceSetName, Map<AnvilScopeName, Set<QualifiedDeclaredName>>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<AnvilScopeMerges>
    get() = Key

  /**
   * @return all scope merges from any source set
   * @since 0.12.0
   */
  suspend fun all(): List<Map<AnvilScopeName, Set<QualifiedDeclaredName>>> {
    return project.sourceSets.keys.map { get(it) }
  }

  /**
   * @return all merged interfaces for this [sourceSetName], grouped by the [AnvilScopeName] for
   *   which they're merged
   * @since 0.12.0
   */
  suspend fun get(sourceSetName: SourceSetName): Map<AnvilScopeName, Set<QualifiedDeclaredName>> {
    return delegate.getOrPut(sourceSetName) {
      project.anvilGraph().get(sourceSetName)
        .mapValues { (_, declarations) ->
          declarations.merges
        }
        .filter { it.value.isNotEmpty() }
    }
  }

  companion object Key : ProjectContext.Key<AnvilScopeMerges> {

    override suspend operator fun invoke(project: McProject): AnvilScopeMerges {
      return AnvilScopeMerges(
        SafeCache(listOf(project.projectPath, AnvilScopeMerges::class)),
        project
      )
    }
  }
}

suspend fun ProjectContext.anvilScopeMerges(): AnvilScopeMerges = get(AnvilScopeMerges)

/**
 * @return all merged interfaces for this [sourceSetName], grouped by the [AnvilScopeName] for which
 *   they're merged
 * @since 0.12.0
 */
suspend fun ProjectContext.anvilScopeMergesForSourceSetName(
  sourceSetName: SourceSetName
): Map<AnvilScopeName, Set<QualifiedDeclaredName>> = anvilScopeMerges().get(sourceSetName)
