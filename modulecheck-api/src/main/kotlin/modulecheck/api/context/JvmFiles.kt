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

package modulecheck.api.context

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.mapNotNull
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.source.JvmFile
import modulecheck.project.JvmFileProvider
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.cache.SafeCache
import java.io.File

/**
 * Represents a collection of JVM files for a project.
 *
 * @property fileFactoryCache a cache for mapping source set names to corresponding JVM file providers
 * @property project the project for which the JVM files are needed
 */
data class JvmFiles(
  internal val fileFactoryCache: SafeCache<SourceSetName, JvmFileProvider>,
  private val project: McProject
) : ProjectContext.Element {

  /**
   * @return the unique key of the JVM files within the project context
   */
  override val key: ProjectContext.Key<JvmFiles>
    get() = Key

  /**
   * Retrieves a flow of JVM files corresponding to the provided source set name.
   *
   * @param sourceSetName the name of the source set for which JVM files are needed
   * @return a flow of [JvmFile]s for the requested source set name
   */
  suspend fun get(sourceSetName: SourceSetName): Flow<JvmFile> {

    return project
      .sourceSets[sourceSetName]
      ?.jvmFiles
      .orEmpty()
      .asFlow()
      .flatMapConcat { directory ->
        directory.walkTopDown()
          .filter { maybeFile -> maybeFile.isFile }
          .asFlow()
          .mapNotNull { file -> getFile(file, sourceSetName) }
      }
  }

  /**
   * Retrieves the JVM file corresponding to the given file and source set name.
   *
   * @param file the file to get the JVM file for
   * @param sourceSetName the name of the source set to which the file belongs
   * @return the [JvmFile] for the requested file, or `null` if not found
   */
  private suspend fun getFile(file: File, sourceSetName: SourceSetName): JvmFile? {

    return fileFactoryCache.getOrPut(sourceSetName) {
      project.jvmFileProviderFactory.create(project, sourceSetName)
    }.getOrNull(file)
  }

  /**
   * Companion object that acts as the key for [JvmFiles] within a [ProjectContext].
   */
  companion object Key : ProjectContext.Key<JvmFiles> {
    /**
     * Generates a [JvmFiles] instance for the given project.
     *
     * @param project the project for which to generate the [JvmFiles] instance
     * @return the generated [JvmFiles] instance
     */
    override suspend operator fun invoke(project: McProject): JvmFiles {

      return JvmFiles(SafeCache(listOf(project.projectPath, JvmFiles::class)), project)
    }
  }
}

/**
 * Retrieves [JvmFiles] from the [ProjectContext].
 * @return the [JvmFiles] instance from the context
 */
suspend fun ProjectContext.jvmFiles(): JvmFiles = get(JvmFiles)

/**
 * Retrieves a flow of JVM files corresponding to the provided source set name from the [ProjectContext].
 *
 * @param sourceSetName the name of the source set for which JVM files are needed
 * @return a flow of [JvmFile]s for the requested source set name
 */
suspend fun ProjectContext.jvmFilesForSourceSetName(sourceSetName: SourceSetName): Flow<JvmFile> =
  jvmFiles().get(sourceSetName)
