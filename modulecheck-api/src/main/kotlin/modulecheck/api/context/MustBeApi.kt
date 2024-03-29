/*
 * Copyright (C) 2021-2023 Rick Busarow
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

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.ConfiguredDependency
import modulecheck.model.dependency.ConfiguredDependency.Companion.copy
import modulecheck.model.dependency.ExternalDependency
import modulecheck.model.dependency.ProjectDependency
import modulecheck.model.dependency.apiConfig
import modulecheck.model.dependency.isTestingOnly
import modulecheck.model.dependency.withUpstream
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.McName
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.UnqualifiedAndroidResourceReferenceName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.project
import modulecheck.utils.coroutines.any
import modulecheck.utils.coroutines.filterAsync
import modulecheck.utils.coroutines.flatMapListConcat
import modulecheck.utils.coroutines.mapAsync
import modulecheck.utils.flatMapToSet
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.lazyDeferred
import modulecheck.utils.lazy.lazySet

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
            .filter { (_, projectDependencyPath) ->

              val contributions = project.projectCache
                .getValue(projectDependencyPath)
                .anvilScopeContributionsForSourceSetName(sourceSetName)

              mergedScopeNames.any { contributions.containsKey(it) }
            }
            .filterNot { it.configurationName == ConfigurationName.api }

          val importsFromDependencies = project.referencesFromDependencies(sourceSetName)

          val directApiProjects = project.projectDependencies[sourceSetName.apiConfig()]
            .orEmpty()
            .toSet()

          val directMainDependencies by lazy {
            project.projectDependencies[sourceSetName].map { it.project(project) }
          }

          mainDependencies
            .asSequence()
            // Anything with an `api` config must be inherited,
            // and will be handled by the InheritedDependencyRule.
            .filterNot { it.configurationName.isApi() }
            .plus(scopeContributingProjects)
            .distinctBy { it.projectPath }
            .filterNot { cpd ->
              // exclude anything which is inherited but already included in local `api` deps
              cpd.copy(configurationName = cpd.configurationName.apiVariant()) in directApiProjects
            }
            .filterAsync {
              !sourceSetName.isTestingOnly(project.sourceSets) && it.project(project)
                .mustBeApiIn(
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
                      declared.projectPath == cpd.projectPath &&
                        declared.isTestFixture == cpd.isTestFixture
                    }
                    // If that didn't work, look for something where the project matches
                    // (which means it's testFixtures)
                    ?: dependencies.firstOrNull { it.projectPath == cpd.projectPath }
                }
                ?: project.dependencySources()
                  .sourceOfOrNull(
                    dependencyProjectPath = cpd.projectPath,
                    sourceSetName = sourceSetName,
                    isTestFixture = cpd.isTestFixture
                  )
              InheritedDependencyWithSource(cpd, source)
            }
            .toList()
            .distinctBy { it.projectDependency }
            .toSet()
        }

      return MustBeApi(apiSet)
    }
  }
}

private suspend fun McProject.referencesFromDependencies(
  sourceSetName: SourceSetName
): Set<ReferenceName> {
  return sourceSetName.withUpstream(this)
    .flatMapToSet { sourceSetOrUpstream ->

      val declarationsInProject = declarations()
        .get(sourceSetOrUpstream, includeUpstream = true)

      jvmFilesForSourceSetName(sourceSetOrUpstream)
        .flatMapListConcat { jvmFile ->

          jvmFile.apiReferences.await()
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
  if (sourceSetName.isTestingOnly(dependentProject.sourceSets)) return false

  val referencesFromDependencies = dependentProject.referencesFromDependencies(sourceSetName)
  val directDependencies = sourceSetName.withUpstream(dependentProject)
    .flatMap { sourceSetOrUpstream ->
      dependentProject.projectDependencies[sourceSetOrUpstream]
        .map { it.project() }
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
  referencesFromDependencies: Set<ReferenceName>,
  sourceSetName: SourceSetName,
  isTestFixtures: Boolean,
  directMainDependencies: List<McProject>
): Boolean {
  suspend fun McProject.declarations(isTestFixtures: Boolean): LazySet<DeclaredName> {
    return if (isTestFixtures) {
      declarations().get(SourceSetName.TEST_FIXTURES, includeUpstream = false)
    } else {
      sourceSetName.withUpstream(dependentProject)
        .map { declarations().get(it, false) }
        .let { lazySet(it) }
    }
  }

  val declarations = declarations(isTestFixtures)

  val rTypeMatcher = "^R(?:\\.\\w+)?$".toRegex()

  val (rTypes, nonRTypeReferences) = referencesFromDependencies
    .partition { it is UnqualifiedAndroidResourceReferenceName }

  nonRTypeReferences
    .firstOrNull { ref ->
      declarations.contains(ref)
    }
    ?.also { return true }

  val rTypesFromExisting: LazyDeferred<Set<McName>> = lazyDeferred {
    directMainDependencies
      .mapAsync { directProject ->
        directProject.declarations(isTestFixtures = false)
          .filter { rType -> rTypeMatcher.matches(rType.name) }
      }
      .flattenMerge()
      .toSet()
  }

  return rTypes.asFlow()
    .filter { declarations.contains(it) }
    .any { rReference -> rTypesFromExisting.await().contains(rReference) }
}

/**
 * @return Returns a [ConfiguredDependency] with an `-api` variant configuration
 *   if the dependency should be `api`, or `-implementation` otherwise.
 * @since 0.12.0
 */
suspend inline fun <reified T : ConfiguredDependency> T.maybeAsApi(dependentProject: McProject): T {
  val mustBeApi = when (val dep: ConfiguredDependency = this@maybeAsApi) {
    is ExternalDependency -> false
    is ProjectDependency -> when {
      configurationName.isKapt() -> false
      else -> dep.project(dependentProject.projectCache)
        .mustBeApiIn(
          dependentProject = dependentProject,
          sourceSetName = configurationName.toSourceSetName(),
          isTestFixtures = isTestFixture
        )
    }
  }

  val newConfig = when {
    mustBeApi -> configurationName.apiVariant()
    configurationName.isKapt() -> configurationName.kaptVariant()
    else -> configurationName.implementationVariant()
  }

  return copy(configurationName = newConfig) as T
}

data class InheritedDependencyWithSource(
  val projectDependency: ProjectDependency,
  val source: ProjectDependency?
)

suspend fun ProjectContext.mustBeApi(): MustBeApi = get(MustBeApi)
