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

package modulecheck.core.context

import modulecheck.api.ConfigurationName
import modulecheck.api.Project2
import modulecheck.api.context.ProjectContext
import modulecheck.api.main
import modulecheck.api.sourceOf
import modulecheck.core.internal.uses
import modulecheck.core.overshot.OvershotDependencyFinding
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class OvershotDependencies(
  internal val delegate: ConcurrentMap<ConfigurationName, Set<OvershotDependencyFinding>>
) : ConcurrentMap<ConfigurationName, Set<OvershotDependencyFinding>> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<OvershotDependencies>
    get() = Key

  companion object Key : ProjectContext.Key<OvershotDependencies> {
    override operator fun invoke(project: Project2): OvershotDependencies {
      val unused = project[UnusedDependencies]
        .main()
        .map { it.cpp() }
        .toSet()

      val apiFromUnused = unused
        .flatMap { cpp ->
          cpp
            .project
            .projectDependencies
            .value["api"]
            .orEmpty()
        }.toSet()

      val unusedPaths = unused
        .map { it.project.path }
        .toSet()

      val mainDependenciesPaths = project
        .projectDependencies
        .value
        .main()
        .map { it.project.path }
        .toSet()

      val grouped = apiFromUnused
        .asSequence()
        .filterNot { it.project.path in unusedPaths }
        .filterNot { it.project.path in mainDependenciesPaths }
        .filter { inheritedDependencyProject -> project.uses(inheritedDependencyProject) }
        .distinct()
        .map { overshot ->

          val source = project
            .sourceOf(overshot)

          val sourceConfig = project
            .projectDependencies
            .value
            .main()
            .firstOrNull { it.project == source }
            ?.configurationName
            ?: "api"

          OvershotDependencyFinding(
            dependentPath = project.path,
            buildFile = project.buildFile,
            dependencyProject = overshot.project,
            dependencyPath = overshot.project.path,
            configurationName = sourceConfig,
            from = source
          )
        }
        .groupBy { it.configurationName }
        .mapValues { it.value.toSet() }

      return OvershotDependencies(ConcurrentHashMap(grouped))
    }
  }
}

val ProjectContext.overshotDependencies: OvershotDependencies get() = get(OvershotDependencies)
