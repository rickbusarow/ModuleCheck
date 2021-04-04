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

import modulecheck.api.Project2
import modulecheck.api.SourceSetName
import modulecheck.api.anvil.AnvilScopeName

data class AnvilScopeContributions(
  internal val delegate: Map<SourceSetName, Map<AnvilScopeName, Set<DeclarationName>>>
) : Map<SourceSetName, Map<AnvilScopeName, Set<DeclarationName>>> by delegate,
    ProjectContext.Element {

  override val key: ProjectContext.Key<AnvilScopeContributions>
    get() = Key

  companion object Key : ProjectContext.Key<AnvilScopeContributions> {

    override operator fun invoke(project: Project2): AnvilScopeContributions {
      val map = project.anvilGraph.scopeContributions

      return AnvilScopeContributions(map)
    }
  }
}

val ProjectContext.anvilScopeContributions: AnvilScopeContributions
  get() = get(AnvilScopeContributions)

fun ProjectContext.anvilScopeContributionsForSourceSetName(
  sourceSetName: SourceSetName
): Map<AnvilScopeName, Set<DeclarationName>> = anvilScopeContributions[sourceSetName].orEmpty()
