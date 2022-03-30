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

import kotlinx.coroutines.flow.toList
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.source.Reference
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.ProjectContext.Element
import modulecheck.utils.LazySet
import modulecheck.utils.SafeCache
import modulecheck.utils.lazySet
import modulecheck.utils.mapAsync

data class References(
  private val delegate: SafeCache<SourceSetName, LazySet<Reference>>,
  private val project: McProject
) : Element {

  override val key: ProjectContext.Key<References>
    get() = Key

  suspend fun all(): LazySet<Reference> {

    return project.sourceSets
      .keys
      .mapAsync { get(it) }
      .toList()
      .let { lazySet(it) }
  }

  suspend fun get(sourceSetName: SourceSetName): LazySet<Reference> {
    return delegate.getOrPut(sourceSetName) { fetchNewReferences(sourceSetName) }
  }

  private suspend fun fetchNewReferences(sourceSetName: SourceSetName): LazySet<Reference> {

    val allLazy = project.jvmFilesForSourceSetName(sourceSetName)
      .toList()
      .plus(project.layoutFilesForSourceSetName(sourceSetName))
      .plus(listOfNotNull(project.manifestFileForSourceSetName(sourceSetName)))
      .flatMap { it.references() }

    return lazySet(allLazy)
  }

  companion object Key : ProjectContext.Key<References> {
    override suspend operator fun invoke(project: McProject): References {

      return References(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.references(): References = get(References)

suspend fun ProjectContext.referencesForSourceSetName(
  sourceSetName: SourceSetName
): LazySet<Reference> = references().get(sourceSetName)
