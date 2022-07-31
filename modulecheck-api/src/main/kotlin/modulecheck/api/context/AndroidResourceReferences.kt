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

import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.source.AndroidResourceReferenceName
import modulecheck.parsing.source.ReferenceName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.isAndroid
import modulecheck.utils.cache.SafeCache
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.dataSource
import modulecheck.utils.lazy.emptyLazySet
import modulecheck.utils.lazy.lazySet
import modulecheck.utils.lazy.toLazySet

data class AndroidResourceReferences(
  private val delegate: SafeCache<SourceSetName, LazySet<ReferenceName>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<AndroidResourceReferences>
    get() = Key

  suspend fun get(sourceSetName: SourceSetName): LazySet<ReferenceName> {
    return delegate.getOrPut(sourceSetName) { fetchNewReferences(sourceSetName) }
  }

  private suspend fun fetchNewReferences(sourceSetName: SourceSetName): LazySet<ReferenceName> {

    if (!project.isAndroid()) return emptyLazySet()

    val jvm = project.jvmFilesForSourceSetName(sourceSetName)
      .map { jvmFile ->

        dataSource {
          jvmFile.references
            .filterIsInstance<AndroidResourceReferenceName>()
            .toSet()
        }
      }
      .toList()
      .toLazySet()

    val layout = project.layoutFilesForSourceSetName(sourceSetName)
      .map { it.references }

    val styles = project.androidStylesFilesForSourceSetName(sourceSetName)
      .map { it.references }

    val manifest = project.manifestFileForSourceSetName(sourceSetName)
      ?.references

    val all = if (manifest != null) {
      layout + styles + manifest + jvm
    } else {
      layout + styles + jvm
    }

    return lazySet(all)
  }

  companion object Key : ProjectContext.Key<AndroidResourceReferences> {
    override suspend operator fun invoke(project: McProject): AndroidResourceReferences {

      return AndroidResourceReferences(
        SafeCache(listOf(project.path, AndroidResourceReferences::class)),
        project
      )
    }
  }
}

suspend fun ProjectContext.androidResourceReferencesForSourceSetName(
  sourceSetName: SourceSetName
): LazySet<ReferenceName> = get(AndroidResourceReferences).get(sourceSetName)
