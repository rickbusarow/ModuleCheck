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

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.mapNotNull
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.source.JvmFile
import modulecheck.project.JvmFileProvider
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.SafeCache
import java.io.File

data class JvmFiles(
  internal val fileFactoryCache: SafeCache<SourceSetName, JvmFileProvider>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<JvmFiles>
    get() = Key

  suspend fun get(sourceSetName: SourceSetName): Flow<JvmFile> {
    @OptIn(FlowPreview::class)
    return project
      .sourceSets[sourceSetName]
      ?.jvmFiles
      .orEmpty()
      .asFlow()
      .flatMapConcat { directory ->
        directory.walkTopDown()
          .filter { maybeFile -> maybeFile.isFile }
          // Only use Sequence/Flow APIs here so that everything is lazy.
          .asFlow()
          .mapNotNull { file -> getFile(file, sourceSetName) }
      }
  }

  private suspend fun getFile(
    file: File,
    sourceSetName: SourceSetName
  ): JvmFile? {

    return fileFactoryCache.getOrPut(sourceSetName) {
      project.jvmFileProviderFactory.create(project, sourceSetName)
    }.getOrNull(file)
  }

  companion object Key : ProjectContext.Key<JvmFiles> {
    override suspend operator fun invoke(project: McProject): JvmFiles {

      return JvmFiles(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.jvmFiles(): JvmFiles = get(JvmFiles)
suspend fun ProjectContext.jvmFilesForSourceSetName(
  sourceSetName: SourceSetName
): Flow<JvmFile> = jvmFiles().get(sourceSetName)
