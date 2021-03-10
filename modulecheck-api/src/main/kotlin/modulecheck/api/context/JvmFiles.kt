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

import modulecheck.api.JvmFile
import modulecheck.api.Project2
import modulecheck.api.SourceSetName
import modulecheck.api.files.jvmFiles
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class JvmFiles(
  internal val delegate: ConcurrentMap<SourceSetName, List<JvmFile>>
) : ConcurrentMap<SourceSetName, List<JvmFile>> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<JvmFiles>
    get() = Key

  companion object Key : ProjectContext.Key<JvmFiles> {
    override operator fun invoke(project: Project2): JvmFiles {
      val map = project
        .sourceSets
        .map { (name, sourceSet) ->

          name to project
            .jvmSourcesForSourceSetName(sourceSet.name)
            .jvmFiles(project.bindingContextForSourceSetName(sourceSet.name))
        }.toMap()

      return JvmFiles(ConcurrentHashMap(map))
    }
  }
}

val ProjectContext.jvmFiles: JvmFiles get() = get(JvmFiles)
fun ProjectContext.jvmFilesForSourceSetName(sourceSetName: SourceSetName): List<JvmFile> =
  jvmFiles[sourceSetName].orEmpty()
