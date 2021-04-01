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

import modulecheck.api.*
import modulecheck.api.context.*
import modulecheck.api.files.KotlinFile

data class MustBeApi(
  internal val delegate: Set<ConfiguredProjectDependency>
) : Set<ConfiguredProjectDependency> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<MustBeApi>
    get() = Key

  companion object Key : ProjectContext.Key<MustBeApi> {
    override operator fun invoke(project: Project2): MustBeApi {
      val mainDependencies = project.allPublicClassPathDependencyDeclarations()

      val mergedScopeNames = project
        .anvilScopeMerges
        .values
        .flatMap { it.keys }

      val scopeContributingProjects = mainDependencies
        .filter { (_, projectDependency) ->

          val contributions =
            projectDependency.anvilScopeContributionsForSourceSetName(SourceSetName.MAIN)

          mergedScopeNames.any { contributions.containsKey(it) }
        }

      val declarationsInProject = project[Declarations][SourceSetName.MAIN]
        .orEmpty()

      val noIdeaWhereTheyComeFrom = project
        .jvmFilesForSourceSetName(SourceSetName.MAIN)
        .filterIsInstance<KotlinFile>()
        .flatMap { kotlinFile ->

          kotlinFile
            .apiReferences
            .filterNot { it in declarationsInProject }
        }.toSet()

      val api = mainDependencies
        .plus(scopeContributingProjects)
        .filterNot { cpd ->
          cpd in project.projectDependencies
            .value[ConfigurationName.api]
            .orEmpty()
        }
        .filter { cpp ->
          cpp
            .project[Declarations][SourceSetName.MAIN]
            .orEmpty()
            .any { declared ->
              declared in noIdeaWhereTheyComeFrom
            }
        }
        .toSet()

      return MustBeApi(api)
    }
  }
}

val ProjectContext.mustBeApi: MustBeApi get() = get(MustBeApi)
