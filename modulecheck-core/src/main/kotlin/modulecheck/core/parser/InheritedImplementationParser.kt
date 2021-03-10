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

import modulecheck.api.ConfiguredProjectDependency
import modulecheck.api.Project2
import modulecheck.api.main
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
          project.sourceOf(ConfiguredProjectDependency("api", overshot.project))
            ?: project.sourceOf(
              ConfiguredProjectDependency("implementation", overshot.project)
            )
        val sourceConfig = project
          .projectDependencies
          .value
          .main()
          .firstOrNull { it.project == source }
          ?.configurationName ?: "api"

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
      grouped.getOrDefault("androidTest", mutableSetOf()),
      grouped.getOrDefault("api", mutableSetOf()),
      grouped.getOrDefault("compileOnly", mutableSetOf()),
      grouped.getOrDefault("implementation", mutableSetOf()),
      grouped.getOrDefault("runtimeOnly", mutableSetOf()),
      grouped.getOrDefault("testApi", mutableSetOf()),
      grouped.getOrDefault("testImplementation", mutableSetOf())
    )
  }
}
