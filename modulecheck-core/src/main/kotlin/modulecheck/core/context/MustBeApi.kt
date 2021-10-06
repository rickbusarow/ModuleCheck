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
import modulecheck.parsing.psi.asDeclaractionName

data class MustBeApi(
  internal val delegate: Set<InheritedDependencyWithSource>
) : Set<InheritedDependencyWithSource> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<MustBeApi>
    get() = Key

  companion object Key : ProjectContext.Key<MustBeApi> {
    override operator fun invoke(project: Project2): MustBeApi {
      val mainDependencies = project.publicDependencies

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
        .filterNot { it.configurationName == ConfigurationName.api }

      val declarationsInProject = project[Declarations][SourceSetName.MAIN]
        .orEmpty()

      val inheritedImports = project
        .jvmFilesForSourceSetName(SourceSetName.MAIN)
        .filterIsInstance<KotlinFile>()
        .flatMap { kotlinFile ->

          kotlinFile
            .apiReferences
            .filterNot { it.asDeclaractionName() in declarationsInProject }
        }.toSet()

      val api = mainDependencies
        .asSequence()
        // .filterNot { it.configurationName == ConfigurationName.api }
        .plus(scopeContributingProjects)
        .filterNot { cpd ->

          cpd in project.projectDependencies
            .value[ConfigurationName.api]
            .orEmpty()
        }
        .filter { cpd ->
          cpd
            .project[Declarations][SourceSetName.MAIN]
            .orEmpty()
            .map { it.fqName }
            .any { declared ->

              declared in inheritedImports ||
                declared in project.imports[SourceSetName.MAIN].orEmpty()
            }
        }
        .map { cpd ->
          val source = project
            .projectDependencies
            .value
            .main()
            .firstOrNull { it.project == cpd.project }
            ?: ConfigurationName
              .main()
              .asSequence()
              .mapNotNull { configName ->
                project.sourceOf(ConfiguredProjectDependency(configName, cpd.project))
              }
              .firstOrNull()
          InheritedDependencyWithSource(cpd, source)
        }
        .distinctBy { it.configuredProjectDependency }
        .toSet()

      return MustBeApi(api)
    }
  }
}

data class InheritedDependencyWithSource(
  val configuredProjectDependency: ConfiguredProjectDependency,
  val source: ConfiguredProjectDependency?
)

val ProjectContext.mustBeApi: MustBeApi get() = get(MustBeApi)
