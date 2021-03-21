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

package modulecheck.core.parser

import modulecheck.api.*
import modulecheck.core.InheritedImplementationDependencyFinding
import modulecheck.core.Parsed
import modulecheck.core.internal.uses

object InheritedImplementationParser : Parser<InheritedImplementationDependencyFinding>() {

  override fun parse(project: Project2): Parsed<InheritedImplementationDependencyFinding> {
    val inherited = project.allPublicClassPathDependencyDeclarations()

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

        val source =
          project.sourceOf(
            ConfiguredProjectDependency(
              "api".asConfigurationName(),
              overshot.project
            )
          )
            ?: project.sourceOf(
              ConfiguredProjectDependency("implementation".asConfigurationName(), overshot.project)
            )
        val sourceConfig = project
          .projectDependencies
          .value
          .main()
          .firstOrNull { it.project == source }
          ?.configurationName ?: "api".asConfigurationName()

        InheritedImplementationDependencyFinding(
          dependentPath = project.path,
          buildFile = project.buildFile,
          dependencyProject = overshot.project,
          dependencyPath = overshot.project.path,
          configurationName = sourceConfig,
          from = source
        )
      }
      .groupBy { it.configurationName }
      .mapValues { it.value.toMutableSet() }

    return Parsed(
      grouped.getOrDefault("androidTest".asConfigurationName(), mutableSetOf()),
      grouped.getOrDefault("api".asConfigurationName(), mutableSetOf()),
      grouped.getOrDefault("compileOnly".asConfigurationName(), mutableSetOf()),
      grouped.getOrDefault("implementation".asConfigurationName(), mutableSetOf()),
      grouped.getOrDefault("runtimeOnly".asConfigurationName(), mutableSetOf()),
      grouped.getOrDefault("testApi".asConfigurationName(), mutableSetOf()),
      grouped.getOrDefault("testImplementation".asConfigurationName(), mutableSetOf())
    )
  }
}
