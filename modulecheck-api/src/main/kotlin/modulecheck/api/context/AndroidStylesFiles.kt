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

import modulecheck.parsing.android.AndroidStylesFile
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.cache.SafeCache

data class AndroidStylesFiles(
  private val delegate: SafeCache<SourceSetName, Set<AndroidStylesFile>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<AndroidStylesFiles>
    get() = Key

  suspend fun all(): Map<SourceSetName, Set<AndroidStylesFile>> {
    return project.sourceSets
      .mapValues { (sourceSetName, _) ->
        get(sourceSetName)
      }
  }

  suspend fun get(sourceSetName: SourceSetName): Set<AndroidStylesFile> {

    return delegate.getOrPut(sourceSetName) {

      val sourceSet = project.sourceSets[sourceSetName] ?: return@getOrPut emptySet()
      sourceSet
        .resourceFiles
        .map { AndroidStylesFile(it) }
        .toSet()
    }
  }

  companion object Key : ProjectContext.Key<AndroidStylesFiles> {
    override suspend operator fun invoke(project: McProject): AndroidStylesFiles {

      return AndroidStylesFiles(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.androidStylesFiles(): AndroidStylesFiles = get(AndroidStylesFiles)
suspend fun ProjectContext.androidStylesFilesForSourceSetName(
  sourceSetName: SourceSetName
): Set<AndroidStylesFile> = androidStylesFiles().get(sourceSetName)
