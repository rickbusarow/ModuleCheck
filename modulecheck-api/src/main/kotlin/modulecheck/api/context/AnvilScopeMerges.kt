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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class AnvilScopeMerges(
  internal val delegate: ConcurrentHashMap<SourceSetName, Map<AnvilScopeName, Set<DeclarationName>>>
) : ConcurrentMap<SourceSetName, Map<AnvilScopeName, Set<DeclarationName>>> by delegate,
    ProjectContext.Element {

  override val key: ProjectContext.Key<AnvilScopeMerges>
    get() = Key

  companion object Key : ProjectContext.Key<AnvilScopeMerges> {

    private val annotations = listOf(
      "com.squareup.anvil.annotations.MergeComponent",
      "com.squareup.anvil.annotations.MergeSubcomponent"
    )

    override operator fun invoke(project: Project2): AnvilScopeMerges {
      if (project.anvilGradlePlugin == null) return AnvilScopeMerges(ConcurrentHashMap())

      val map = project.parseAnvilScopes(annotations)

      return AnvilScopeMerges(ConcurrentHashMap(map))
    }
  }
}

val ProjectContext.anvilScopeMerges: AnvilScopeMerges
  get() = get(AnvilScopeMerges)

fun ProjectContext.anvilScopeMergesForSourceSetName(
  sourceSetName: SourceSetName
): Map<AnvilScopeName, Set<DeclarationName>> = anvilScopeMerges[sourceSetName].orEmpty()
