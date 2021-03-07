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

import modulecheck.api.Project2
import modulecheck.api.SourceSetName
import modulecheck.api.XmlFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class LayoutFiles(
  internal val delegate: ConcurrentMap<SourceSetName, Set<XmlFile.LayoutFile>>
) : ConcurrentMap<SourceSetName, Set<XmlFile.LayoutFile>> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<LayoutFiles>
    get() = Key

  companion object Key : ProjectContext.Key<LayoutFiles> {
    override operator fun invoke(project: Project2): LayoutFiles {
      val map = project
        .sourceSets
        .mapValues { (_, sourceSet) ->
          sourceSet
            .layoutFiles
            .map { XmlFile.LayoutFile(it) }
            .toSet()
        }

      return LayoutFiles(ConcurrentHashMap(map))
    }
  }
}

val ProjectContext.layoutFiles: LayoutFiles get() = get(LayoutFiles)
fun  ProjectContext.layoutFilesForSourceSetName(sourceSetName: SourceSetName): Set<XmlFile.LayoutFile> =
  layoutFiles[sourceSetName].orEmpty()
