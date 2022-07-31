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

import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.source.AndroidRDeclaredName
import modulecheck.parsing.source.AndroidResourceDeclaredName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.isAndroid
import modulecheck.utils.cache.SafeCache

data class AndroidRDeclaredNames(
  private val delegate: SafeCache<SourceSetName, AndroidRDeclaredName?>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<AndroidRDeclaredNames>
    get() = Key

  suspend fun all(): Set<AndroidRDeclaredName> {
    return project.sourceSets.keys
      .mapNotNull { get(it) }
      .toSet()
  }

  suspend fun get(sourceSetName: SourceSetName): AndroidRDeclaredName? {
    if (!project.isAndroid()) return null

    return delegate.getOrPut(sourceSetName) {
      project.androidBasePackagesForSourceSetName(sourceSetName)
        ?.let { AndroidResourceDeclaredName.r(it) }
    }
  }

  companion object Key : ProjectContext.Key<AndroidRDeclaredNames> {
    override suspend operator fun invoke(project: McProject): AndroidRDeclaredNames {
      return AndroidRDeclaredNames(
        SafeCache(listOf(project.path, AndroidRDeclaredNames::class)), project
      )
    }
  }
}

suspend fun ProjectContext.androidRDeclaredNames() = get(AndroidRDeclaredNames)

suspend fun ProjectContext.androidRDeclaredNameForSourceSetName(
  sourceSetName: SourceSetName
): AndroidRDeclaredName? = androidRDeclaredNames().get(sourceSetName)
