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

import modulecheck.model.sourceset.SourceSetName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.cache.SafeCache
import java.io.File

/**
 * All JVM source files for a specific source set in a project.
 *
 * @property delegate a [SafeCache] instance which manages a
 *   cache of source set names to the associated JVM source files.
 * @property project the [McProject] for which the source files are retrieved.
 */
data class JvmSourceFiles(
  private val delegate: SafeCache<SourceSetName, Set<File>>,
  private val project: McProject
) : ProjectContext.Element {

  /** Key used to identify this [JvmSourceFiles] instance within [ProjectContext]. */
  override val key: ProjectContext.Key<JvmSourceFiles>
    get() = Key

  /**
   * Retrieves the JVM source files for a given source set.
   *
   * @param sourceSetName The name of the source set for which to retrieve the JVM source files.
   * @return A set of [File]s representing the JVM source files in the requested source set.
   */
  suspend fun get(sourceSetName: SourceSetName): Set<File> {
    return delegate.getOrPut(sourceSetName) {
      val sourceSet = project.sourceSets[sourceSetName] ?: return@getOrPut emptySet()

      sourceSet.jvmFiles
    }
  }

  /** Key object used to identify [JvmSourceFiles] within [ProjectContext]. */
  companion object Key : ProjectContext.Key<JvmSourceFiles> {
    /**
     * Creates a new instance of [JvmSourceFiles] for a given project.
     *
     * @param project The project for which to create the [JvmSourceFiles] instance.
     * @return A new instance of [JvmSourceFiles].
     */
    override suspend operator fun invoke(project: McProject): JvmSourceFiles {

      return JvmSourceFiles(SafeCache(listOf(project.projectPath, JvmSourceFiles::class)), project)
    }
  }
}

/** @return The [JvmSourceFiles] instance for this context. */
suspend fun ProjectContext.jvmSourceFiles(): JvmSourceFiles = get(JvmSourceFiles)

/**
 * @param sourceSetName The name of the source set for which to retrieve the JVM source files.
 * @return A set of [File]s representing the JVM source files in the requested source set.
 */
suspend fun ProjectContext.jvmSourcesForSourceSetName(sourceSetName: SourceSetName): Set<File> =
  jvmSourceFiles().get(sourceSetName)
