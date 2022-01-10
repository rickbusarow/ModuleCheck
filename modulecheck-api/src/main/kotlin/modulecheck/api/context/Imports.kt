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
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.source.Reference
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.LazySet
import modulecheck.utils.LazySet.DataSource
import modulecheck.utils.LazySet.DataSource.Priority.HIGH
import modulecheck.utils.SafeCache
import modulecheck.utils.asDataSource
import modulecheck.utils.lazySet

data class Imports(
  private val delegate: SafeCache<SourceSetName, LazySet<Reference>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<Imports>
    get() = Key

  suspend fun get(sourceSetName: SourceSetName): LazySet<Reference> {
    return delegate.getOrPut(sourceSetName) {

      val jvm: List<DataSource<Reference>> = project.get(JvmFiles)
        .get(sourceSetName)
        .map { it.importsLazy.asDataSource(HIGH) }
        .toList()

      val layout = project.get(LayoutFiles)
        .get(sourceSetName)
        .map { it.customViews.asDataSource() }

      lazySet(sources = layout + jvm)
    }
  }

  companion object Key : ProjectContext.Key<Imports> {
    override suspend operator fun invoke(project: McProject): Imports {

      return Imports(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.imports(): Imports = get(Imports)
suspend fun ProjectContext.importsForSourceSetName(
  sourceSetName: SourceSetName
): LazySet<Reference> {
  return imports().get(sourceSetName)
}
