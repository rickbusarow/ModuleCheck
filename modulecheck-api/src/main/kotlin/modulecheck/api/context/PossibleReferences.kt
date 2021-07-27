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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

typealias PossibleReferenceName = String

data class PossibleReferences(
  internal val delegate: ConcurrentMap<SourceSetName, Set<ImportName>>
) : ConcurrentMap<SourceSetName, Set<ImportName>> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<PossibleReferences>
    get() = Key

  companion object Key : ProjectContext.Key<PossibleReferences> {
    override operator fun invoke(project: Project2): PossibleReferences {
      val map = project
        .sourceSets
        .mapValues { (_, sourceSet) ->
          project[JvmFiles][sourceSet.name]
            .orEmpty()
            .flatMap { jvmFile -> jvmFile.maybeExtraReferences }
            .toSet()
        }

      return PossibleReferences(ConcurrentHashMap(map))
    }
  }
}

val ProjectContext.possibleReferences: PossibleReferences get() = get(PossibleReferences)
fun ProjectContext.possibleReferencesForSourceSetName(
  sourceSetName: SourceSetName
): Set<PossibleReferenceName> =
  possibleReferences[sourceSetName].orEmpty()
