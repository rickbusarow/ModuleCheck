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
import modulecheck.api.ConfiguredProjectDependency
import modulecheck.api.Project2
import modulecheck.api.context.ProjectContext
import modulecheck.api.context.anvilScopeContributionsForSourceSetName
import modulecheck.api.context.anvilScopeMergesForSourceSetName
import modulecheck.core.DependencyFinding
import modulecheck.core.internal.uses
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.LazyThreadSafetyMode.NONE

data class UnusedDependency(
  override val dependentPath: String,
  override val buildFile: File,
  override val dependencyProject: Project2,
  override val dependencyIdentifier: String,
  override val configurationName: ConfigurationName
) : DependencyFinding("unused") {
  fun cpd() = ConfiguredProjectDependency(configurationName, dependencyProject)
}

data class UnusedDependencies(
  internal val delegate: ConcurrentMap<ConfigurationName, Set<UnusedDependency>>
) : ConcurrentMap<ConfigurationName, Set<UnusedDependency>> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<UnusedDependencies>
    get() = Key

  companion object Key : ProjectContext.Key<UnusedDependencies> {
    override operator fun invoke(project: Project2): UnusedDependencies {
      val neededForScopes by lazy(NONE) { project.anvilScopeMap() }

      val unusedHere = project
        .sourceSets
        .flatMap { it.key.configurationNames() }
        .asSequence()
        .flatMap { config ->
          project
            .projectDependencies
            .value[config]
            .orEmpty()
        }
        .filterNot { cpd ->
          // test configurations have the main source project as a dependency.
          // without this, every project will report itself as unused.
          cpd.project.path == project.path
        }
        .filterNot { cpd -> project.uses(cpd) }
        .filterNot { cpd ->
          cpd.project in neededForScopes[cpd.configurationName].orEmpty()
        }

      val grouped = unusedHere.map { cpp ->

        UnusedDependency(
          dependentPath = project.path,
          buildFile = project.buildFile,
          dependencyProject = cpp.project,
          dependencyIdentifier = cpp.project.path,
          configurationName = cpp.configurationName
        )
      }
        .groupBy { it.configurationName }
        .mapValues { it.value.toSet() }

      return UnusedDependencies(ConcurrentHashMap(grouped))
    }

    private fun Project2.anvilScopeMap(): Map<ConfigurationName, List<Project2>> {
      if (anvilGradlePlugin == null) {
        return mapOf()
      }

      return configurations
        .map { (configurationName, _) ->
          val merged = anvilScopeMergesForSourceSetName(configurationName.toSourceSetName())

          val configurationDependencies = projectDependencies
            .value[configurationName]
            .orEmpty()
            .toSet()

          val neededForScopeInConfig = configurationDependencies
            .filter { cpd ->

              val contributed = cpd
                .project
                .anvilScopeContributionsForSourceSetName(cpd.configurationName.toSourceSetName())

              contributed.any { cont ->
                cont.key in merged.keys
              }
            }
            .map { it.project }

          configurationName to neededForScopeInConfig
        }
        .toMap()
    }
  }
}

val ProjectContext.unusedDependencies: UnusedDependencies get() = get(UnusedDependencies)
