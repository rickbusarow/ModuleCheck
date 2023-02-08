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

import kotlinx.coroutines.flow.toList
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.source.ReferenceName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.ProjectContext.Element
import modulecheck.utils.cache.SafeCache
import modulecheck.utils.coroutines.mapAsync
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.lazySet
import modulecheck.utils.lazy.toLazySet

data class References(
  private val delegate: SafeCache<SourceSetName, LazySet<ReferenceName>>,
  private val project: McProject
) : Element {

  override val key: ProjectContext.Key<References>
    get() = Key

  suspend fun all(): LazySet<ReferenceName> {

    return project.sourceSets
      .keys
      .mapAsync { get(it) }
      .toList()
      .let { lazySet(it) }
  }

  suspend fun get(sourceSetName: SourceSetName): LazySet<ReferenceName> {
    return delegate.getOrPut(sourceSetName) { fetchNewReferences(sourceSetName) }
  }

  private suspend fun fetchNewReferences(sourceSetName: SourceSetName): LazySet<ReferenceName> {

    return project.jvmFilesForSourceSetName(sourceSetName)
      .toList()
      .plus(project.layoutFilesForSourceSetName(sourceSetName))
      .plus(project.androidStylesFilesForSourceSetName(sourceSetName))
      .plus(listOfNotNull(project.manifestFileForSourceSetName(sourceSetName)))
      .map { it.references }
      .toLazySet()
  }

  companion object Key : ProjectContext.Key<References> {
    override suspend operator fun invoke(project: McProject): References {

      return References(
        SafeCache(listOf(project.projectPath, References::class)),
        project
      )
    }
  }
}

suspend fun ProjectContext.references(): References = get(References)

suspend fun ProjectContext.referencesForSourceSetName(
  sourceSetName: SourceSetName
): LazySet<ReferenceName> = references().get(sourceSetName)
