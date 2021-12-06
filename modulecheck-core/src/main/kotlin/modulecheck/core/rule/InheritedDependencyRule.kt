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

package modulecheck.core.rule

import kotlinx.coroutines.flow.toList
import modulecheck.api.context.classpathDependencies
import modulecheck.api.rule.ModuleCheckRule
import modulecheck.api.settings.ChecksSettings
import modulecheck.core.InheritedDependencyFinding
import modulecheck.core.context.mustBeApiIn
import modulecheck.core.internal.uses
import modulecheck.project.McProject
import modulecheck.project.SourceSetName
import modulecheck.utils.mapAsync

class InheritedDependencyRule : ModuleCheckRule<InheritedDependencyFinding> {

  override val id = "InheritedDependency"
  override val description = "Finds project dependencies which are used in the current module, " +
    "but are not actually directly declared as dependencies in the current module"

  override suspend fun check(project: McProject): List<InheritedDependencyFinding> {

    val mainDirectDependencies = project.projectDependencies.main()
      .map { it.project to it.isTestFixture }
      .toSet()

    val used = project.classpathDependencies().all()
      .filterNot { mainDirectDependencies.contains(it.contributed.project to it.contributed.isTestFixture) }
      .distinctBy { it.contributed.project.path to it.contributed.isTestFixture }
      .filter { project.uses(it) }

    val dependencyPathCache = mutableMapOf<SourceSetName, Set<Pair<String, Boolean>>>()
    fun pathsForSourceSet(sourceSetName: SourceSetName): Set<Pair<String, Boolean>> {
      return dependencyPathCache.getOrPut(sourceSetName) {
        project.projectDependencies[sourceSetName]
          .map { it.project.path to it.isTestFixture }
          .toSet()
      }
    }

    return used.asSequence()
      .filterNot {
        pathsForSourceSet(it.source.configurationName.toSourceSetName())
          .contains((it.contributed.project.path to it.contributed.isTestFixture))
      }
      .distinct()
      .mapAsync { transitive ->

        val source = transitive.source
        val inherited = transitive.contributed

        val mustBeApi = inherited.configurationName
          .toSourceSetName() == SourceSetName.MAIN && inherited.project
          .mustBeApiIn(project, inherited.isTestFixture)

        val newConfig = if (mustBeApi) {
          source.configurationName.apiVariant()
        } else {
          source.configurationName
        }

        InheritedDependencyFinding(
          dependentProject = project,
          newDependency = inherited.copy(newConfig),
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

  override fun shouldApply(checksSettings: ChecksSettings): Boolean {
    return checksSettings.inheritedDependency
  }
}
