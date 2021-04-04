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

data class AnvilScopeMerges(
  internal val delegate: Map<SourceSetName, Map<AnvilScopeName, Set<DeclarationName>>>
) : Map<SourceSetName, Map<AnvilScopeName, Set<DeclarationName>>> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<AnvilScopeMerges>
    get() = Key

  companion object Key : ProjectContext.Key<AnvilScopeMerges> {

    override operator fun invoke(project: Project2): AnvilScopeMerges {
      val map = project.anvilGraph.scopeMerges

      return AnvilScopeMerges(map)
    }
  }
}

val ProjectContext.anvilScopeMerges: AnvilScopeMerges
  get() = get(AnvilScopeMerges)

fun ProjectContext.anvilScopeMergesForSourceSetName(
  sourceSetName: SourceSetName
): Map<AnvilScopeName, Set<DeclarationName>> = anvilScopeMerges[sourceSetName].orEmpty()
