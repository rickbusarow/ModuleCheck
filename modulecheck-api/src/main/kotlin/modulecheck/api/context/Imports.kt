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
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.SafeCache
import modulecheck.utils.flatMapSetConcat

typealias ReferenceName = String

data class Imports(
  private val delegate: SafeCache<SourceSetName, Set<ReferenceName>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<Imports>
    get() = Key

  suspend fun get(sourceSetName: SourceSetName): Set<ReferenceName> {
    return delegate.getOrPut(sourceSetName) {
      val jvm = project.get(JvmFiles)
        .get(sourceSetName)
        .flatMapSetConcat { it.imports }
      val layout = project.get(LayoutFiles)
        .get(sourceSetName)
        .flatMap { it.customViews }
        .toSet()

      jvm + layout
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
): Set<ReferenceName> {
  return imports().get(sourceSetName)
}
