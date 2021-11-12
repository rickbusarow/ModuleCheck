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

import modulecheck.parsing.McProject
import modulecheck.parsing.ProjectContext
import modulecheck.parsing.SourceSetName
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class ClassPaths(
  internal val delegate: ConcurrentMap<SourceSetName, Set<File>>
) : ConcurrentMap<SourceSetName, Set<File>> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<ClassPaths>
    get() = Key

  companion object Key : ProjectContext.Key<ClassPaths> {
    override suspend operator fun invoke(project: McProject): ClassPaths {
      val map = project
        .sourceSets
        .values
        .associate { sourceSet ->
          sourceSet.name to (sourceSet.classpathFiles + sourceSet.outputFiles).toSet()
        }

      return ClassPaths(ConcurrentHashMap(map))
    }
  }
}

suspend fun ProjectContext.classPaths(): ClassPaths = get(ClassPaths)
suspend fun ProjectContext.classpathForSourceSetName(sourceSetName: SourceSetName): Set<File> =
  classPaths()[sourceSetName].orEmpty()
