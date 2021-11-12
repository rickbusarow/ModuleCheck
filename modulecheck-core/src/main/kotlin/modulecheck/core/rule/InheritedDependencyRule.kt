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

import modulecheck.api.ModuleCheckRule
import modulecheck.api.context.classpathDependencies
import modulecheck.api.settings.ChecksSettings
import modulecheck.api.util.mapBlocking
import modulecheck.core.InheritedDependencyFinding
import modulecheck.core.context.mustBeApiIn
import modulecheck.core.internal.uses
import modulecheck.parsing.McProject
import modulecheck.parsing.SourceSetName

class InheritedDependencyRule : ModuleCheckRule<InheritedDependencyFinding> {

  override val id = "InheritedDependency"
  override val description = "Finds project dependencies which are used in the current module, " +
    "but are not actually directly declared as dependencies in the current module"

  override suspend fun check(project: McProject): List<InheritedDependencyFinding> {

    val mainDirectDependencies = project.projectDependencies.main()
      .map { it.project }
      .toSet()

    val used = project.classpathDependencies().all()
      .filterNot { it.contributed.project in mainDirectDependencies }
      .distinctBy { it.contributed.project.path }
      .filter { project.uses(it) }

    val dependencyPathCache = mutableMapOf<SourceSetName, Set<String>>()
    fun pathsForSourceSet(sourceSetName: SourceSetName): Set<String> {
      return dependencyPathCache.getOrPut(sourceSetName) {
        project.projectDependencies[sourceSetName].map { it.project.path }.toSet()
      }
    }

    return used.asSequence()
      .filterNot { it.contributed.project.path in pathsForSourceSet(it.source.configurationName.toSourceSetName()) }
      .distinct()
      .mapBlocking { transitive ->

        val source = transitive.source
        val inherited = transitive.contributed

        val mustBeApi = inherited.project.mustBeApiIn(project)

        val newConfig = if (mustBeApi) {
          source.configurationName.apiVariant()
        } else {
          source.configurationName
        }

        InheritedDependencyFinding(
          dependentPath = project.path,
          dependentProject = project,
          buildFile = project.buildFile,
          dependencyProject = inherited.project,
          dependencyPath = inherited.project.path,
          configurationName = newConfig,
          source = source
        )
      }
      .groupBy { it.configurationName }
      .mapValues { (_, findings) ->
        findings.distinctBy { it.source.isTestFixture to it.dependencyPath }
          .sorted()
      }
      .values
      .flatten()
  }

  override fun shouldApply(checksSettings: ChecksSettings): Boolean {
    return checksSettings.inheritedDependency
  }
}
