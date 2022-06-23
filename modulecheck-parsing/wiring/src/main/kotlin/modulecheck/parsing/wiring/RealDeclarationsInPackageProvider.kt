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

package modulecheck.parsing.wiring

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import modulecheck.api.context.jvmFilesForSourceSetName
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.psi.internal.DeclarationsInPackageProvider
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.PackageName
import modulecheck.project.McProject
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.dataSource
import modulecheck.utils.lazy.toLazySet

class RealDeclarationsInPackageProvider(
  private val project: McProject
) : DeclarationsInPackageProvider {
  override suspend fun get(
    sourceSetName: SourceSetName,
    packageName: PackageName
  ): LazySet<DeclaredName> {
    return project.jvmFilesForSourceSetName(sourceSetName)
      .filter { it.packageName == packageName }
      .map { dataSource { it.declarations } }
      .toList()
      .toLazySet()
  }

  override suspend fun getWithUpstream(
    sourceSetName: SourceSetName,
    packageName: PackageName
  ): LazySet<DeclaredName> {
    return sourceSetName.withUpstream(project)
      .map { sourceSetOrUpstream ->
        get(sourceSetOrUpstream, packageName)
      }
      .toLazySet()
  }
}
