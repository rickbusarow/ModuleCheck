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

import modulecheck.api.ConfiguredProjectDependency
import modulecheck.api.Project2
import modulecheck.api.SourceSetName
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class ResolvedReferences(
  internal val delegate: ConcurrentMap<SourceSetName, Set<ConfiguredProjectDependency>>
) : ConcurrentMap<SourceSetName, Set<ConfiguredProjectDependency>> by delegate,
    ProjectContext.Element {

  override val key: ProjectContext.Key<ResolvedReferences>
    get() = Key

  companion object Key : ProjectContext.Key<ResolvedReferences> {
    override operator fun invoke(project: Project2): ResolvedReferences {

      val map = project
        .sourceSets
        .mapValues { (_, sourceSet) ->

          val projectDependencies = project
            .projectDependencies
            .value[sourceSet.name]
            .orEmpty()

          project[JvmFiles][sourceSet.name]
            .orEmpty()
            .flatMap { jvmFile ->

              jvmFile
                .maybeExtraReferences
                .mapNotNull { possible ->
                  projectDependencies
                    .firstOrNull {
                      it.project[Declarations]["main"].orEmpty()
                        .any { it == possible }
                    }
                }
            }
            .toSet()
        }

      return ResolvedReferences(ConcurrentHashMap(map))
    }
  }
}
