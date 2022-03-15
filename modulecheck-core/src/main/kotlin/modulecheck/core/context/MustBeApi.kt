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
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import modulecheck.api.context.anvilGraph
import modulecheck.api.context.anvilScopeContributionsForSourceSetName
import modulecheck.api.context.classpathDependencies
import modulecheck.api.context.declarations
import modulecheck.api.context.dependencySources
import modulecheck.api.context.jvmFilesForSourceSetName
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.source.DeclarationName
import modulecheck.parsing.source.JavaFile
import modulecheck.parsing.source.KotlinFile
import modulecheck.parsing.source.contains
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.LazyDeferred
import modulecheck.utils.LazySet
import modulecheck.utils.any
import modulecheck.utils.emptyDataSource
import modulecheck.utils.filterAsync
import modulecheck.utils.flatMapListConcat
import modulecheck.utils.flatMapToSet
import modulecheck.utils.lazyDeferred
import modulecheck.utils.lazySet
import modulecheck.utils.mapAsync

data class MustBeApi(
  private val delegate: Set<InheritedDependencyWithSource>
) : Set<InheritedDependencyWithSource> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<MustBeApi>
    get() = Key

  companion object Key : ProjectContext.Key<MustBeApi> {
    override suspend operator fun invoke(project: McProject): MustBeApi {

      val apiSet = project.sourceSets
        .keys
        .flatMapToSet { sourceSetName ->

          // this is anything in the classpath, including inherited dependencies
          val mainDependencies = project.classpathDependencies()
            .get(sourceSetName)
            .map { it.contributed }

          val mergedScopeNames = project.anvilGraph()
            .mergedScopeNames()

          // projects with a @Contributes(...) annotation somewhere
          val scopeContributingProjects = mainDependencies
            .filter { (_, projectDependency) ->

              val contributions =
                projectDependency.anvilScopeContributionsForSourceSetName(sourceSetName)

              mergedScopeNames.any { contributions.containsKey(it) }
            }
            .filterNot { it.configurationName == ConfigurationName.api }

          val importsFromDependencies = project.referencesFromDependencies(sourceSetName)

          val directApiProjects = project.projectDependencies[sourceSetName.apiConfig()]
            .orEmpty()
            .toSet()

          val directMainDependencies by lazy {
            project.projectDependencies[sourceSetName].map { it.project }
          }

          mainDependencies
            .asSequence()
            // Anything with an `api` config must be inherited,
            // and will be handled by the InheritedDependencyRule.
            .filterNot { it.configurationName.isApi() }
            .plus(scopeContributingProjects)
            .distinctBy { it.project }
            .filterNot { cpd ->
              // exclude anything which is inherited but already included in local `api` deps
              cpd in directApiProjects
            }
            .filterAsync {

              !sourceSetName.isTestingOnly() && it.project.mustBeApiIn(
                dependentProject = project,
                referencesFromDependencies = importsFromDependencies,
                sourceSetName = it.configurationName.toSourceSetName(),
                isTestFixtures = it.isTestFixture,
                directMainDependencies = directMainDependencies
              )
            }
            .map { cpd ->
              val source = project
                .projectDependencies[sourceSetName]
                .let { dependencies ->

                  // First try to find a normal "implementation" version of the dependency.
                  dependencies
                    .firstOrNull { declared ->
                      declared.project == cpd.project && declared.isTestFixture == cpd.isTestFixture
                    }
                    // If that didn't work, look for something where the project matches
                    // (which means it's testFixtures)
                    ?: dependencies.firstOrNull { it.project == cpd.project }
                }
                ?: project.dependencySources().sourceOfOrNull(
                  dependencyProjectPath = cpd.project.path,
                  sourceSetName = sourceSetName,
                  isTestFixture = cpd.isTestFixture
                )
              InheritedDependencyWithSource(cpd, source)
            }
            .toList()
            .distinctBy { it.configuredProjectDependency }
            .toSet()
        }

      return MustBeApi(apiSet)
    }
  }
}

private suspend fun McProject.referencesFromDependencies(
  sourceSetName: SourceSetName
): Set<String> {

  return sourceSetName.withUpstream(this)
    .flatMapToSet { sourceSetOrUpstream ->

      val declarationsInProject = declarations()
        .get(sourceSetOrUpstream, includeUpstream = true)

      jvmFilesForSourceSetName(sourceSetOrUpstream)
        .flatMapListConcat { jvmFile ->

          when (jvmFile) {
            is JavaFile -> jvmFile.apiReferences.map { it.asString() }
            is KotlinFile -> jvmFile.apiReferences.await()
          }
            .filterNot { declarationsInProject.contains(it) }
        }
    }
}

suspend fun McProject.mustBeApiIn(
  dependentProject: McProject,
  sourceSetName: SourceSetName,
  isTestFixtures: Boolean
): Boolean {

  // `testApi` and `androidTestApi` are not valid configurations
  if (sourceSetName.isTestingOnly()) return false

  val referencesFromDependencies = dependentProject.referencesFromDependencies(sourceSetName)
  val directDependencies = sourceSetName.withUpstream(dependentProject)
    .flatMap { sourceSetOrUpstream ->
      dependentProject.projectDependencies[sourceSetOrUpstream]
        .map { it.project }
    }
  return mustBeApiIn(
    dependentProject = dependentProject,
    referencesFromDependencies = referencesFromDependencies,
    sourceSetName = sourceSetName,
    isTestFixtures = isTestFixtures,
    directMainDependencies = directDependencies
  )
}

private suspend fun McProject.mustBeApiIn(
  dependentProject: McProject,
  referencesFromDependencies: Set<String>,
  sourceSetName: SourceSetName,
  isTestFixtures: Boolean,
  directMainDependencies: List<McProject>
): Boolean {

  suspend fun McProject.declarations(isTestFixtures: Boolean): LazySet<DeclarationName> {
    return if (isTestFixtures) {
      declarations().get(SourceSetName.TEST_FIXTURES, includeUpstream = false)
    } else {
      sourceSetName.withUpstream(dependentProject)
        .map { declarations().get(it, false) }
        .let { lazySet(it, emptyDataSource()) }
    }
  }

  val declarations = declarations(isTestFixtures)

  val rTypeMatcher = "^R(?:\\.\\w+)?$".toRegex()

  val (rTypes, nonRTypeReferences) = referencesFromDependencies
    .partition { rTypeMatcher.matches(it) }

  nonRTypeReferences
    .firstOrNull { ref -> declarations.contains(ref) }
    ?.let { return true }

  val rTypesFromExisting: LazyDeferred<Set<DeclarationName>> = lazyDeferred {
    directMainDependencies
      .mapAsync { directProject ->
        directProject.declarations(isTestFixtures = false)
          .filter { rType -> rTypeMatcher.matches(rType.fqName) }
      }
      .flattenMerge()
      .toSet()
  }

  return rTypes.asFlow()
    .filter { declarations.contains(it) }
    .any { rReference -> rTypesFromExisting.await().contains(rReference) }
}

/**
 * @return Returns a [ConfiguredProjectDependency] with an `-api` variant configuration if the
 *   dependency should be `api`, or `-implementation` otherwise.
 */
suspend fun ConfiguredProjectDependency.asApiOrImplementation(
  dependentProject: McProject
): ConfiguredProjectDependency {

  val mustBeApi = project.mustBeApiIn(
    dependentProject = dependentProject,
    sourceSetName = configurationName.toSourceSetName(),
    isTestFixtures = isTestFixture
  )

  val newConfig = if (mustBeApi) {
    configurationName.apiVariant()
  } else {
    configurationName.implementationVariant()
  }

  return copy(configurationName = newConfig)
}

data class InheritedDependencyWithSource(
  val configuredProjectDependency: ConfiguredProjectDependency,
  val source: ConfiguredProjectDependency?
)

suspend fun ProjectContext.mustBeApi(): MustBeApi = get(MustBeApi)
