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

import modulecheck.api.Config
import modulecheck.api.ConfiguredProjectDependency
import modulecheck.api.Project2
import modulecheck.core.InheritedImplementationDependencyFinding
import modulecheck.core.MCP.Parsed
import modulecheck.core.internal.usedIn
import modulecheck.core.mcp

object InheritedImplementationParser : Parser<InheritedImplementationDependencyFinding>() {

  override fun parse(project: Project2): Parsed<InheritedImplementationDependencyFinding> {
    val mcp = project.mcp()

    val inherited = mcp.allPublicClassPathDependencyDeclarations()

    val used = inherited
      .filter { it.usedIn(mcp) }

    val mainDependenciesPaths = mcp.dependencies
      .main()
      .map { it.project.path }
      .toSet()

    val grouped = used
      .asSequence()
      .filterNot { it.project.path in mainDependenciesPaths }
      .distinct()
      .map { overshot ->

        val source =
          mcp.sourceOf(ConfiguredProjectDependency(Config.Api, overshot.project)) ?: mcp.sourceOf(
            ConfiguredProjectDependency(Config.Implementation, overshot.project)
          )
        val sourceConfig = mcp
          .dependencies
          .main()
          .firstOrNull { it.project == source?.project }
          ?.config

        InheritedImplementationDependencyFinding(
          dependentProject = mcp.project,
          dependencyProject = overshot.project,
          dependencyPath = overshot.project.path,
          config = sourceConfig ?: Config.Api,
          from = source
        )
      }
      .groupBy { it.config }
      .mapValues { it.value.toMutableSet() }

    return Parsed(
      grouped.getOrDefault(Config.AndroidTest, mutableSetOf()),
      grouped.getOrDefault(Config.Api, mutableSetOf()),
      grouped.getOrDefault(Config.CompileOnly, mutableSetOf()),
      grouped.getOrDefault(Config.Implementation, mutableSetOf()),
      grouped.getOrDefault(Config.RuntimeOnly, mutableSetOf()),
      grouped.getOrDefault(Config.TestApi, mutableSetOf()),
      grouped.getOrDefault(Config.TestImplementation, mutableSetOf())
    )
  }
}
