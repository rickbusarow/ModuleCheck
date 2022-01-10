/*
 * Copyright (C) 2021-2022 Rick Busarow
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

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import modulecheck.api.context.anvilGraph
import modulecheck.api.context.anvilScopeContributionsForSourceSetName
import modulecheck.api.context.apiDependencySources
import modulecheck.api.context.classpathDependencies
import modulecheck.api.context.declarations
import modulecheck.api.context.jvmFilesForSourceSetName
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.source.KotlinFile
import modulecheck.parsing.source.asDeclarationName
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.any
import modulecheck.utils.filterAsync
import modulecheck.utils.flatMapListConcat
import modulecheck.utils.flatMapSetConcat
import modulecheck.utils.lazyDeferred
import modulecheck.utils.mapAsync
import kotlin.LazyThreadSafetyMode.NONE

data class MustBeApi(
  private val delegate: Set<InheritedDependencyWithSource>
) : Set<InheritedDependencyWithSource> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<MustBeApi>
    get() = Key

  companion object Key : ProjectContext.Key<MustBeApi> {
    override suspend operator fun invoke(project: McProject): MustBeApi {
      // this is anything in the main classpath, including inherited dependencies
      val mainDependencies = project.classpathDependencies()
        .get(SourceSetName.MAIN)
        .map { it.contributed }

      val mergedScopeNames = project.anvilGraph()
        .mergedScopeNames()

      // projects with a @Contributes(...) annotation somewhere
      val scopeContributingProjects = mainDependencies
        .filter { (_, projectDependency) ->

          val contributions =
            projectDependency.anvilScopeContributionsForSourceSetName(SourceSetName.MAIN)

          mergedScopeNames.any { contributions.containsKey(it) }
        }
        .filterNot { it.configurationName == ConfigurationName.api }

      val importsFromDependencies = project.referencesFromDependencies()

      val directApiProjects = project.projectDependencies[ConfigurationName.api]
        .orEmpty()
        .map { it.project }
        .toSet()

      val directMainDependencies by lazy(NONE) {
        project.projectDependencies.main().map { it.project }
      }

      val api = mainDependencies
        .asSequence()
        // Anything with an `api` config must be inherited,
        // and will be handled by the InheritedDependencyRule.
        .filterNot { it.configurationName == ConfigurationName.api }
        .plus(scopeContributingProjects)
        .distinctBy { it.project }
        .filterNot { cpd ->
          // exclude anything which is inherited but already included in local `api` deps
          cpd.project in directApiProjects
        }
        .filterAsync {
          it.project.mustBeApiIn(
            referencesFromDependencies = importsFromDependencies,
            isTestFixtures = it.isTestFixture,
            directMainDependencies = directMainDependencies
          )
        }
        .map { cpd ->
          val source = project
            .projectDependencies
            .main()
            .firstOrNull { it.project == cpd.project }
            ?: project.apiDependencySources().sourceOfOrNull(
              dependencyProjectPath = cpd.project.path,
              sourceSetName = SourceSetName.MAIN,
              isTestFixture = cpd.isTestFixture
            )
          InheritedDependencyWithSource(cpd, source)
        }
        .toList()
        .distinctBy { it.configuredProjectDependency }
        .toSet()

      return MustBeApi(api)
    }
  }
}

private suspend fun McProject.referencesFromDependencies(): Set<String> {

  val declarationsInProject = declarations()
    .get(SourceSetName.MAIN)

  return jvmFilesForSourceSetName(SourceSetName.MAIN)
    .filterIsInstance<KotlinFile>()
    .flatMapListConcat { kotlinFile ->

      kotlinFile
        .apiReferences
        .await()
        .filterNot { it.asDeclarationName() in declarationsInProject }
      // .map { it.asString() }
    }.toSet()
}

suspend fun McProject.mustBeApiIn(
  dependentProject: McProject,
  isTestFixtures: Boolean
): Boolean {
  val referencesFromDependencies = dependentProject.referencesFromDependencies()
  val directMainDependencies = dependentProject.projectDependencies.main()
    .map { it.project }
  return mustBeApiIn(
    referencesFromDependencies = referencesFromDependencies,
    isTestFixtures = isTestFixtures,
    directMainDependencies = directMainDependencies
  )
}

private suspend fun McProject.mustBeApiIn(
  referencesFromDependencies: Set<String>,
  isTestFixtures: Boolean,
  directMainDependencies: List<McProject>
): Boolean {

  suspend fun McProject.declarations(isTestFixtures: Boolean): Set<String> {
    return if (isTestFixtures) {
      declarations().get(SourceSetName.TEST_FIXTURES)
    } else {
      declarations().get(SourceSetName.MAIN)
    }
      .map { it.fqName }
      .toSet()
  }

  val declarations = declarations(isTestFixtures)

  val rTypeMatcher = "^R(?:\\.[a-zA-Z0-9_]+)?$".toRegex()

  val (rTypes, nonRTypeReferences) = referencesFromDependencies
    .partition { rTypeMatcher.matches(it) }

  val apiFromRProperties = nonRTypeReferences
    .any { ref -> ref in declarations }

  if (apiFromRProperties) return true

  val rTypesFromExisting = lazyDeferred {
    directMainDependencies
      .mapAsync { directProject ->
        directProject.declarations(isTestFixtures = false)
          .filter { rType -> rTypeMatcher.matches(rType) }
      }
      .flatMapSetConcat { it.toSet() }
  }

  return rTypes.asFlow()
    .filter { it in declarations }
    .any { rReference -> rReference in rTypesFromExisting.await() }
}

data class InheritedDependencyWithSource(
  val configuredProjectDependency: ConfiguredProjectDependency,
  val source: ConfiguredProjectDependency?
)

suspend fun ProjectContext.mustBeApi(): MustBeApi = get(MustBeApi)
