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

package modulecheck.core.rule

import kotlinx.coroutines.flow.toList
import modulecheck.api.context.classpathDependencies
import modulecheck.api.rule.ModuleCheckRule
import modulecheck.api.settings.ChecksSettings
import modulecheck.core.InheritedDependencyFinding
import modulecheck.core.context.asApiOrImplementation
import modulecheck.core.internal.uses
import modulecheck.parsing.gradle.ProjectPath.StringProjectPath
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.gradle.sortedByInheritance
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject
import modulecheck.project.TransitiveProjectDependency
import modulecheck.utils.flatMapToSet
import modulecheck.utils.mapAsync

class InheritedDependencyRule : ModuleCheckRule<InheritedDependencyFinding> {

  override val id = "InheritedDependency"
  override val description = "Finds project dependencies which are used in the current module, " +
    "but are not actually directly declared as dependencies in the current module"

  override suspend fun check(project: McProject): List<InheritedDependencyFinding> {

    data class SourceSetDependency(
      val sourceSetName: SourceSetName,
      val path: StringProjectPath,
      val isTestFixture: Boolean
    )

    fun ConfiguredProjectDependency.toSourceSetDependency(
      sourceSetName: SourceSetName = configurationName.toSourceSetName(),
      path: StringProjectPath = this@toSourceSetDependency.path,
      isTestFixture: Boolean = this@toSourceSetDependency.isTestFixture
    ) = SourceSetDependency(
      sourceSetName = sourceSetName,
      path = path,
      isTestFixture = isTestFixture
    )

    // For each source set, the set of all module paths and whether they're test fixtures
    val dependencyPathCache = mutableMapOf<SourceSetName, Set<SourceSetDependency>>()

    // Returns true if the dependency is already declared in this exact source set,
    // **or** if it's declared in an upstream source set.
    //
    // For example, this function will return true for a `testImplementation` configured dependency
    // which is already declared in the main source set (such as with `api` or `implementation`).
    fun alreadyInLocalClasspath(cpd: ConfiguredProjectDependency): Boolean {
      fun dependencyPathsForSourceSet(
        sourceSetName: SourceSetName
      ): Set<SourceSetDependency> {
        return dependencyPathCache.getOrPut(sourceSetName) {
          project.projectDependencies[sourceSetName]
            .map { it.toSourceSetDependency() }
            .toSet()
        }
      }

      return cpd.configurationName.toSourceSetName()
        // Check the cpd's source set first, but if the dependency isn't defined there,
        // also check the upstream configurations.
        .withUpstream(project)
        .any { sourceSetName ->

          dependencyPathsForSourceSet(sourceSetName)
            .contains(cpd.toSourceSetDependency(sourceSetName))
        }
    }

    val candidates = project.classpathDependencies()
      .all()
      .asSequence()
      // Projects shouldn't inherit themselves.  This false-positive can happen if a test
      // fixture/utilities module depends upon a module, and that module uses the test module in
      // testImplementation.
      .filterNot { transitive -> transitive.contributed.project == project }
      .flatMap { transitive ->

        /*
        If a transitive dependency is used in the same configuration as its source, then that's
        the configuration which should be used.

        Given this config:
        ┌────────┐                      ┌────────┐          ┌────────┐
        │ :lib3  │──testImplementation─▶│ :lib2  │────api──▶│ :lib1  │
        └────────┘                      └────────┘          └────────┘

        We'd want to check whether :lib3 uses :lib1, but with the `testImplementation`
        configuration.  We don't want to check whether :lib3 uses :lib1 in `api`, because :lib2
        only provides it to `testImplementation`.

        We want to see whether this configuration is valid:
                                         ┌────────┐          ┌────────┐
                             ┌──────────▶│ :lib2  │────api──▶│ :lib1  │
                    testImplementation   └────────┘          ▲────────┘
        ┌────────┐───────────┘                               │
        │ :lib3  │                                           │
        └────────┘───────────────────testImplementation──────┘
        However, that transitive dependency is also providing the contributed dependency to other
        source sets which depend upon it. So, check the downstream SourceSets as well.
        */
        transitive
          .withContributedConfiguration(transitive.source.configurationName.implementationVariant())
          // from `main` source set, get a sequence of [main, test, debug, release, testDebug, ...]
          .withDownstreamVariants(project)
          // if the transitive contributed dependency is testFixtures, check the main source as well
          .withTestFixturesMainSource()
          // Sorting by inheritance can be very expensive even for a single module's source sets,
          // if the variant/flavor/build type matrix is complex.  So filter out duplicates first,
          // even though more filtering may be done below after the flatMap.
          .filterNot { alreadyInLocalClasspath(it.contributed) }
          .distinctBy { it.contributed.toSourceSetDependency() }
          // check main before debug, debug before androidTestDebug, etc.
          .sortedByInheritance(project)
      }
      .distinctBy { it.contributed.toSourceSetDependency() }
      .groupBy { it.contributed.configurationName.toSourceSetName() }

    val visitedSourceSetMap = mutableMapOf<SourceSetName, List<TransitiveProjectDependency>>()

    project.sourceSets.values
      .sortedByInheritance()
      .forEach { sourceSet ->

        val allUpstreamTransitive = sourceSet.upstream
          .flatMapToSet { visitedSourceSetMap.getValue(it) }

        val forThisSourceSet = candidates[sourceSet.name].orEmpty()
          .mapNotNull { candidateTransitive ->

            val contributed = candidateTransitive.contributed

            val alreadyUpstream = allUpstreamTransitive.any { (_, upstreamCpd) ->

              if (contributed.isTestFixture && !upstreamCpd.isTestFixture) {
                return@any false
              }

              upstreamCpd.project == contributed.project &&
                contributed.isTestFixture == upstreamCpd.isTestFixture
            }

            candidateTransitive.takeIf { !alreadyUpstream && project.uses(it.contributed) }
          }

        visitedSourceSetMap[sourceSet.name] = forThisSourceSet
      }

    return visitedSourceSetMap.values.flatten()
      .mapAsync { (source, inherited) ->

        InheritedDependencyFinding(
          dependentProject = project,
          newDependency = inherited.asApiOrImplementation(project),
          source = source
        )
      }
      .toList()
      .groupBy { it.configurationName }
      .mapValues { (_, findings) ->
        findings
          .distinctBy { it.newDependency }
          .sorted()
      }
      .values
      .flatten()
  }

  /**
   * Returns a sequence starting with the receiver's configuration, then all **downstream**
   * configurations.
   *
   * For example, if we're checking to see if a transitive dependency is used in `main`, we should
   * also check whether it's used in the source sets which inherit from `main` (like `debug`,
   * `release`, `androidTest`, `test`, etc.).
   */
  private fun TransitiveProjectDependency.withDownstreamVariants(
    project: McProject
  ): Sequence<TransitiveProjectDependency> {

    return source.configurationName
      .toSourceSetName()
      .withDownStream(project)
      .asSequence()
      .map { it.implementationConfig() }
      .map { configName -> withContributedConfiguration(configName) }
  }

  private fun Sequence<TransitiveProjectDependency>.sortedByInheritance(
    project: McProject
  ): Sequence<TransitiveProjectDependency> {

    return sortedWith { o1, o2 ->
      val o1SourceSet = o1.contributed.declaringSourceSetName()
      val o2SourceSet = o2.contributed.declaringSourceSetName()

      o2SourceSet.inheritsFrom(o1SourceSet, project).compareTo(true)
    }
  }

  /**
   * @return a sequence containing all original transitive dependencies, but adds `main` contributed
   *   dependencies where the original transitive dependency was providing `main` via `testFixtures`.
   */
  private fun Sequence<TransitiveProjectDependency>.withTestFixturesMainSource():
    Sequence<TransitiveProjectDependency> {

    return flatMap { transitiveCpd ->
      sequence {
        yield(transitiveCpd)
        if (transitiveCpd.contributed.isTestFixture) {
          yield(
            transitiveCpd.copy(
              contributed = transitiveCpd.contributed.copy(isTestFixture = false)
            )
          )
        }
      }
    }
  }

  override fun shouldApply(checksSettings: ChecksSettings): Boolean {
    return checksSettings.inheritedDependency
  }
}
