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

import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.cache.SafeCache
import java.io.File

data class ResSourceFiles(
  private val delegate: SafeCache<SourceSetName, Set<File>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<ResSourceFiles>
    get() = Key

  suspend fun all(): Set<File> {
    return project.sourceSets.keys
      .flatMap { get(it) }
      .toSet()
  }

  suspend fun get(sourceSetName: SourceSetName): Set<File> {
    return delegate.getOrPut(sourceSetName) {
      project.sourceSets[sourceSetName]
        ?.resourceFiles
        .orEmpty()
    }
  }

  companion object Key : ProjectContext.Key<ResSourceFiles> {
    override suspend operator fun invoke(project: McProject): ResSourceFiles {
      return ResSourceFiles(SafeCache(listOf(project.path, ResSourceFiles::class)), project)
    }
  }
}

suspend fun ProjectContext.resSourceFiles(): ResSourceFiles = get(ResSourceFiles)
suspend fun ProjectContext.resourceFilesForSourceSetName(sourceSetName: SourceSetName): Set<File> =
  resSourceFiles().get(sourceSetName)
