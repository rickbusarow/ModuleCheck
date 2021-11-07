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

import modulecheck.api.context.Declarations
import modulecheck.api.context.anvilScopeContributionsForSourceSetName
import modulecheck.api.context.anvilScopeMerges
import modulecheck.api.context.jvmFilesForSourceSetName
import modulecheck.api.context.publicDependencies
import modulecheck.parsing.ConfigurationName
import modulecheck.parsing.ConfiguredProjectDependency
import modulecheck.parsing.McProject
import modulecheck.parsing.ProjectContext
import modulecheck.parsing.SourceSetName
import modulecheck.parsing.asDeclarationName
import modulecheck.parsing.psi.KotlinFile
import modulecheck.parsing.sourceOf

data class MustBeApi(
  internal val delegate: Set<InheritedDependencyWithSource>
) : Set<InheritedDependencyWithSource> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<MustBeApi>
    get() = Key

  companion object Key : ProjectContext.Key<MustBeApi> {
    override operator fun invoke(project: McProject): MustBeApi {
      // this is anything in the main classpath, including inherited dependencies
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
            .filterNot { it.asDeclarationName() in declarationsInProject }
        }.toSet()

      val api = mainDependencies
        .asSequence()
        // Anything with an `api` config must be inherited,
        // and will be handled by the InheritedDependencyRule.
        .filterNot { it.configurationName == ConfigurationName.api }
        .plus(scopeContributingProjects)
        .filterNot { cpd ->
          // exclude anything which is inherited but already included in local `api` deps
          cpd in project.projectDependencies[ConfigurationName.api].orEmpty()
        }
        .filter { cpd ->
          cpd
            .project[Declarations][SourceSetName.MAIN]
            .orEmpty()
            .map { it.fqName }
            .any { declared ->

              declared in inheritedImports
            }
        }
        .map { cpd ->
          val source = project
            .projectDependencies
            .main()
            .firstOrNull { it.project == cpd.project }
            ?: ConfigurationName
              .main()
              .asSequence()
              .mapNotNull { configName ->
                project.sourceOf(
                  ConfiguredProjectDependency(
                    configurationName = configName,
                    project = cpd.project,
                    isTestFixture = cpd.isTestFixture
                  )
                )
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
