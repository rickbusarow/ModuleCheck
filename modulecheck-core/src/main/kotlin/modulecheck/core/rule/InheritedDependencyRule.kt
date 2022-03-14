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
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.gradle.names
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject
import modulecheck.project.TransitiveProjectDependency
import modulecheck.utils.mapAsync

class InheritedDependencyRule : ModuleCheckRule<InheritedDependencyFinding> {

  override val id = "InheritedDependency"
  override val description = "Finds project dependencies which are used in the current module, " +
    "but are not actually directly declared as dependencies in the current module"

  override suspend fun check(project: McProject): List<InheritedDependencyFinding> {

    // For each source set, the set of all module paths and whether they're test fixtures
    val dependencyPathCache = mutableMapOf<SourceSetName, Set<Pair<String, Boolean>>>()

    // Returns true if the dependency is already declared in this exact configuration, **or** if
    // it's declared in an upstream configuration.
    //
    // For example, this function will return true for a `testImplementation` configured dependency
    // which is already declared in the main source set (such as with `api` or `implementation`).
    fun ConfiguredProjectDependency.alreadyInClasspath(): Boolean {
      fun dependencyPathsForSourceSet(sourceSetName: SourceSetName): Set<Pair<String, Boolean>> {
        return dependencyPathCache.getOrPut(sourceSetName) {
          project.projectDependencies[sourceSetName]
            .map { it.project.path to it.isTestFixture }
            .toSet()
        }
      }

      return configurationName.toSourceSetName()
        // Check the receiver's configuration first, but if the dependency isn't used there, also
        // check the upstream configurations.
        .withUpstream(project)
        .any { sourceSet ->
          dependencyPathsForSourceSet(sourceSet)
            .contains(this.project.path to isTestFixture)
        }
    }

    // Returns the list of all transitive dependencies where the contributed dependency is used,
    // filtering out any configuration which would be redundant.  For instance, if a dependency is
    // used in `main`, the function will stop there instead of returning a list of `main`, `debug`,
    // `test`, etc.
    suspend fun List<TransitiveProjectDependency>.allUsed(): List<TransitiveProjectDependency> {
      return foldRight(listOf()) { transitiveProjectDependency, alreadyUsed ->

        val contributedSourceSet = transitiveProjectDependency.contributed
          .configurationName
          .toSourceSetName()

        val alreadyUsedUpstream = alreadyUsed.any {
          val usedSourceSet = it.contributed.configurationName.toSourceSetName()
          contributedSourceSet.inheritsFrom(usedSourceSet, project)
        }

        when {
          alreadyUsedUpstream -> alreadyUsed
          project.uses(transitiveProjectDependency.contributed) -> {
            alreadyUsed + transitiveProjectDependency
          }
          else -> alreadyUsed
        }
      }
    }

    val used = project.classpathDependencies().all()
      .distinctBy { it.contributed.project.path to it.contributed.isTestFixture }
      .flatMap { transitive ->

        // If a transitive dependency is used in the same configuration as its source, then that's
        // the configuration which should be used and we're done.  However, that transitive
        // dependency is also providing the dependency to other source sets which depend upon it.
        // So, check the inheriting dependencies as well.
        transitive.withContributedConfiguration(transitive.source.configurationName)
          .withInheritingVariants(project)
          .filterNot { it.contributed.alreadyInClasspath() }
          .toList()
          .sortedByInheritance(project)
          .allUsed()
      }

    return used.asSequence()
      .distinct()
      // Projects shouldn't inherit themselves.  This false-positive can happen if a test
      // fixture/utilities module depends upon a module, and that module uses the test module in
      // testImplementation.
      .filterNot { transitive -> transitive.contributed.project == project }
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
        findings.distinctBy { it.source.isTestFixture to it.newDependency.path }
          .sorted()
      }
      .values
      .flatten()
  }

  private fun TransitiveProjectDependency.withContributedConfiguration(
    configurationName: ConfigurationName
  ): TransitiveProjectDependency {
    val newContributed = contributed.copy(configurationName = configurationName)
    return copy(contributed = newContributed)
  }

  // Returns a sequence starting with the receiver's configuration, then all **downstream**
  // configurations.  This is useful because when we're checking to see if a transitive dependency
  // is used in `main` (for instance), we should also check whether it's used in the source sets
  // which inherit from `main` (like `debug`, `release`, `androidTest`, `test`, etc.).
  private fun TransitiveProjectDependency.withInheritingVariants(
    project: McProject
  ): Sequence<TransitiveProjectDependency> {
    return sequenceOf(this) + project.configurations
      .getValue(source.configurationName)
      .withDownstream()
      .asSequence()
      .names()
      .filter { name -> name.isImplementation() }
      .map { configName -> withContributedConfiguration(configName) }
  }

  private fun List<TransitiveProjectDependency>.sortedByInheritance(
    project: McProject
  ): List<TransitiveProjectDependency> {
    val sorted = toMutableList()

    sorted.sortWith { o1, o2 ->
      val o1SourceSet = o1.contributed.configurationName.toSourceSetName()
      val o2SourceSet = o2.contributed.configurationName.toSourceSetName()

      val inherits = o1SourceSet.inheritsFrom(o2SourceSet, project)
      when {
        inherits -> -1
        o1SourceSet == o2SourceSet -> 0
        else -> 1
      }
    }
    return sorted
  }

  override fun shouldApply(checksSettings: ChecksSettings): Boolean {
    return checksSettings.inheritedDependency
  }
}
