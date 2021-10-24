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

import modulecheck.parsing.Project2
import modulecheck.parsing.ProjectContext
import modulecheck.parsing.SourceSetName
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class JvmSourceFiles(
  internal val delegate: ConcurrentMap<SourceSetName, Set<File>>
) : ConcurrentMap<SourceSetName, Set<File>> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<JvmSourceFiles>
    get() = Key

  companion object Key : ProjectContext.Key<JvmSourceFiles> {
    override operator fun invoke(project: Project2): JvmSourceFiles {
      val map = project
        .sourceSets
        .map { (name, sourceSet) ->

          name to sourceSet.jvmFiles
        }.toMap()

      return JvmSourceFiles(ConcurrentHashMap(map))
    }
  }
}

val ProjectContext.jvmSourceFiles: JvmSourceFiles get() = get(JvmSourceFiles)
fun ProjectContext.jvmSourcesForSourceSetName(sourceSetName: SourceSetName): Set<File> =
  jvmSourceFiles[sourceSetName].orEmpty()
