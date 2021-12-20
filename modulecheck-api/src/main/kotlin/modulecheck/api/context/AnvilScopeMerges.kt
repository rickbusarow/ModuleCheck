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

package modulecheck.api.context

import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.source.DeclarationName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.temp.AnvilScopeName
import modulecheck.utils.SafeCache

data class AnvilScopeMerges(
  private val delegate: SafeCache<SourceSetName, Map<AnvilScopeName, Set<DeclarationName>>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<AnvilScopeMerges>
    get() = Key

  suspend fun all(): List<Map<AnvilScopeName, Set<DeclarationName>>> {
    return project.sourceSets.keys.map { get(it) }
  }

  suspend fun get(sourceSetName: SourceSetName): Map<AnvilScopeName, Set<DeclarationName>> {
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
      return AnvilScopeMerges(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.anvilScopeMerges(): AnvilScopeMerges = get(AnvilScopeMerges)

suspend fun ProjectContext.anvilScopeMergesForSourceSetName(
  sourceSetName: SourceSetName
): Map<AnvilScopeName, Set<DeclarationName>> = anvilScopeMerges().get(sourceSetName)
