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

import modulecheck.api.context.publicDependencies
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.core.InheritedDependencyFinding
import modulecheck.core.context.mustBeApi
import modulecheck.core.internal.uses
import modulecheck.parsing.*
import kotlin.LazyThreadSafetyMode.NONE

class InheritedDependencyRule(
  override val settings: ModuleCheckSettings
) : ModuleCheckRule<InheritedDependencyFinding> {

  override val id = "InheritedDependency"
  override val description = "Finds project dependencies which are used in the current module, " +
    "but are not actually directly declared as dependencies in the current module"

  override fun check(project: McProject): List<InheritedDependencyFinding> {
    val inherited = project.publicDependencies
    val used = inherited
      .filter { project.uses(it) }

    val mainDependenciesPaths = project
      .projectDependencies
      .value
      .main()
      .map { it.project.path }
      .toSet()

    val grouped = used
      .asSequence()
      .filterNot { it.project.path in mainDependenciesPaths }
      .distinct()
      .map { overshot ->

        val source = ConfigurationName
          .main()
          .asSequence()
          .mapNotNull { configName ->
            project.sourceOf(
              ConfiguredProjectDependency(
                configurationName = configName,
                project = overshot.project,
                isTestFixture = overshot.isTestFixture
              )
            )
          }
          .firstOrNull()

        val sourceConfig by lazy(NONE) {
          project
            .projectDependencies
            .value
            .main()
            .firstOrNull { it.project == source?.project }
            ?.configurationName ?: "api".asConfigurationName()
        }

        val mustBeApi = project
          .mustBeApi
          .any { it.configuredProjectDependency.project == overshot.project }

        val newConfig = if (mustBeApi) ConfigurationName.api else sourceConfig

        InheritedDependencyFinding(
          dependentPath = project.path,
          buildFile = project.buildFile,
          dependencyProject = overshot.project,
          dependencyPath = overshot.project.path,
          configurationName = newConfig,
          source = source
        )
      }
      .filterNot { it.dependencyPath in mainDependenciesPaths }
      .groupBy { it.configurationName }
      .mapValues { it.value.toMutableSet() }

    return grouped.values.flatten()
  }
}
