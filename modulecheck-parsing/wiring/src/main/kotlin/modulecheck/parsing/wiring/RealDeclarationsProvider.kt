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

package modulecheck.parsing.wiring

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import modulecheck.api.context.classpathDependencies
import modulecheck.api.context.declarations
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.psi.internal.DeclarationsProvider
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.PackageName
import modulecheck.project.McProject
import modulecheck.project.project
import modulecheck.utils.coroutines.plus
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.toLazySet

class RealDeclarationsProvider(
  private val project: McProject
) : DeclarationsProvider {
  override suspend fun get(
    sourceSetName: SourceSetName,
    packageName: PackageName
  ): LazySet<DeclaredName> {
    return project.declarations()
      .get(sourceSetName, false, packageName)
  }

  override suspend fun getWithUpstream(
    sourceSetName: SourceSetName,
    packageNameOrNull: PackageName?
  ): LazySet<DeclaredName> {

    return flowOf(project)
      .plus(
        project.classpathDependencies()
          .get(sourceSetName)
          .map { it.contributed.project(project) }
          .asFlow()
      )
      .flatMapConcat { projectOrUpstream ->
        projectOrUpstream.declarations()
          .get(sourceSetName, true, packageNameOrNull)
      }
      .toLazySet()
  }
}
