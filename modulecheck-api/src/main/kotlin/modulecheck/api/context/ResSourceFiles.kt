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
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class ResSourceFiles(
  internal val delegate: ConcurrentMap<SourceSetName, Set<File>>
) : ConcurrentMap<SourceSetName, Set<File>> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<ResSourceFiles>
    get() = Key

  companion object Key : ProjectContext.Key<ResSourceFiles> {
    override suspend operator fun invoke(project: McProject): ResSourceFiles {
      val map = project
        .sourceSets
        .mapValues { (_, sourceSet) ->

          sourceSet.resourceFiles
        }

      return ResSourceFiles(ConcurrentHashMap(map))
    }
  }
}

suspend fun ProjectContext.resSourceFiles(): ResSourceFiles = get(ResSourceFiles)
suspend fun ProjectContext.resourcesForSourceSetName(sourceSetName: SourceSetName): Set<File> =
  resSourceFiles()[sourceSetName].orEmpty()
