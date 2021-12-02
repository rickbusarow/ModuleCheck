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

import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.SourceSetName
import modulecheck.utils.SafeCache
import java.io.File

data class ClassPaths(
  private val delegate: SafeCache<SourceSetName, Set<File>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<ClassPaths>
    get() = Key

  suspend fun get(sourceSetName: SourceSetName): Set<File> {
    return delegate.getOrPut(sourceSetName) {
      val sourceSet = project.sourceSets[sourceSetName] ?: return@getOrPut emptySet()

      (sourceSet.classpathFiles + sourceSet.outputFiles).toSet()
    }
  }

  companion object Key : ProjectContext.Key<ClassPaths> {
    override suspend operator fun invoke(project: McProject): ClassPaths {

      return ClassPaths(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.classPaths(): ClassPaths = get(ClassPaths)
suspend fun ProjectContext.classpathForSourceSetName(sourceSetName: SourceSetName): Set<File> =
  classPaths().get(sourceSetName)
