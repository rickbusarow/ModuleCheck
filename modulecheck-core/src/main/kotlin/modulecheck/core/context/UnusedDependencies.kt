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

import modulecheck.api.Deletable
import modulecheck.api.context.anvilScopeContributionsForSourceSetName
import modulecheck.api.context.anvilScopeMergesForSourceSetName
import modulecheck.api.util.filterNotBlocking
import modulecheck.api.util.lazyDeferred
import modulecheck.core.DependencyFinding
import modulecheck.core.internal.uses
import modulecheck.parsing.ConfigurationName
import modulecheck.parsing.ConfiguredProjectDependency
import modulecheck.parsing.McProject
import modulecheck.parsing.ProjectContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class UnusedDependency(
  override val dependentPath: String,
  override val buildFile: File,
  override val dependencyProject: McProject,
  override val dependencyIdentifier: String,
  override val configurationName: ConfigurationName,
  val isTestFixture: Boolean
) : DependencyFinding("unusedDependency"),
  Deletable {

  override val message: String
    get() = "The declared dependency is not used in this module."

  fun cpd() = ConfiguredProjectDependency(
    configurationName = configurationName,
    project = dependencyProject,
    isTestFixture = isTestFixture
  )

  override fun toString(): String {
    return "UnusedDependency(\n" +
      "\tdependentPath='$dependentPath', \n" +
      "\tbuildFile=$buildFile, \n" +
      "\tdependencyProject=$dependencyProject, \n" +
      "\tdependencyIdentifier='$dependencyIdentifier', \n" +
      "\tconfigurationName=$configurationName\n" +
      ")"
  }

  override fun fromStringOrEmpty(): String = ""
}

data class UnusedDependencies(
  internal val delegate: ConcurrentMap<ConfigurationName, Set<UnusedDependency>>
) : ConcurrentMap<ConfigurationName, Set<UnusedDependency>> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<UnusedDependencies>
    get() = Key

  companion object Key : ProjectContext.Key<UnusedDependencies> {
    override suspend operator fun invoke(project: McProject): UnusedDependencies {
      val neededForScopes = lazyDeferred { project.anvilScopeMap() }

      val unusedHere = project
        .sourceSets
        .flatMap { it.key.configurationNames() }
        .asSequence()
        .flatMap { config -> project.projectDependencies[config].orEmpty() }
        .filterNot { cpd ->
          // test configurations have the main source project as a dependency.
          // without this, every project will report itself as unused.
          cpd.project.path == project.path
        }
        .filterNotBlocking { cpd -> project.uses(cpd) }
        .filterNotBlocking { cpd ->
          cpd.project in neededForScopes.await()[cpd.configurationName].orEmpty()
        }

      val grouped = unusedHere.map { cpp ->

        UnusedDependency(
          dependentPath = project.path,
          buildFile = project.buildFile,
          dependencyProject = cpp.project,
          dependencyIdentifier = cpp.project.path,
          configurationName = cpp.configurationName,
          isTestFixture = cpp.isTestFixture
        )
      }
        .groupBy { it.configurationName }
        .mapValues { it.value.toSet() }

      return UnusedDependencies(ConcurrentHashMap(grouped))
    }

    private suspend fun McProject.anvilScopeMap(): Map<ConfigurationName, List<McProject>> {
      if (anvilGradlePlugin == null) {
        return mapOf()
      }

      return configurations
        .map { (configurationName, _) ->
          val merged = anvilScopeMergesForSourceSetName(configurationName.toSourceSetName())

          val configurationDependencies = projectDependencies[configurationName]
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

suspend fun ProjectContext.unusedDependencies(): UnusedDependencies = get(UnusedDependencies)
