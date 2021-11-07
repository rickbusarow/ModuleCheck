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

import modulecheck.parsing.ConfiguredProjectDependency
import modulecheck.parsing.McProject
import modulecheck.parsing.ProjectContext
import modulecheck.parsing.SourceSetName

data class ResolvedReferences(
  internal val delegate: Map<SourceSetName, Set<ConfiguredProjectDependency>>
) : Map<SourceSetName, Set<ConfiguredProjectDependency>> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<ResolvedReferences>
    get() = Key

  companion object Key : ProjectContext.Key<ResolvedReferences> {
    override operator fun invoke(project: McProject): ResolvedReferences {
      val map = mutableMapOf<SourceSetName, MutableSet<ConfiguredProjectDependency>>()

      project
        .configurations
        .mapValues { (configurationName, _) ->

          val projectDependencies = project
            .projectDependencies[configurationName]
            .orEmpty()

          project
            .jvmFilesForSourceSetName(configurationName.toSourceSetName())
            .forEach { jvmFile ->

              val t = jvmFile
                .maybeExtraReferences
                .mapNotNull { possible ->
                  projectDependencies
                    .firstOrNull {
                      it.project
                        .declarations[SourceSetName.MAIN]
                        .orEmpty()
                        .any { it.fqName == possible }
                    }
                }

              map
                .getOrPut(configurationName.toSourceSetName()) { mutableSetOf() }
                .addAll(t)
            }
        }

      return ResolvedReferences(map)
    }
  }
}

val ProjectContext.resolvedReferences: ResolvedReferences get() = get(ResolvedReferences)
fun ProjectContext.resolvedReferencesForSourceSetName(
  sourceSetName: SourceSetName
): Set<ConfiguredProjectDependency> = resolvedReferences[sourceSetName].orEmpty()
