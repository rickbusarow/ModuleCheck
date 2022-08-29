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

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.source.ReferenceName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.cache.SafeCache
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.LazySet.DataSource
import modulecheck.utils.lazy.asDataSource
import modulecheck.utils.lazy.lazySet

data class Imports(
  private val delegate: SafeCache<SourceSetName, LazySet<ReferenceName>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<Imports>
    get() = Key

  suspend fun get(sourceSetName: SourceSetName): LazySet<ReferenceName> {
    return delegate.getOrPut(sourceSetName) {

      val jvm: List<DataSource<ReferenceName>> = project.get(JvmFiles)
        .get(sourceSetName)
        .map { it.importsLazy.asDataSource(DataSource.Priority.HIGH) }
        .toList()

      val layout = project.get(LayoutFiles)
        .get(sourceSetName)
        .map { it.customViews.asDataSource() }

      lazySet(layout + jvm)
    }
  }

  companion object Key : ProjectContext.Key<Imports> {
    override suspend operator fun invoke(project: McProject): Imports {

      return Imports(SafeCache(listOf(project.path, Imports::class)), project)
    }
  }
}

suspend fun ProjectContext.imports(): Imports = get(Imports)
suspend fun ProjectContext.importsForSourceSetName(
  sourceSetName: SourceSetName
): LazySet<ReferenceName> {
  return imports().get(sourceSetName)
}
