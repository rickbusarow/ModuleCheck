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
import modulecheck.api.Project2
import modulecheck.core.MCP
import modulecheck.core.RedundantDependencyFinding
import modulecheck.core.mcp

object RedundantParser : Parser<RedundantDependencyFinding>() {

  override fun parse(project: Project2): MCP.Parsed<RedundantDependencyFinding> {
    val mcp = project.mcp()

    val allMain = mcp.dependencies.api.toSet()

    val inheritedDependencyProjects = mcp.dependencies.main()
      .flatMap {
        it.mcp().allPublicClassPathDependencyDeclarations()
          .map { it.project }
          .toSet()
      }

    val redundant = allMain
      .filter { it.project in inheritedDependencyProjects }
      .map {
        val from = allMain
          .filter { inherited -> inherited.project == it.project }
          .map { it.project }

        RedundantDependencyFinding(
          dependentPath = project.path,
          buildFile = project.buildFile,
          dependencyProject = it.project,
          dependencyPath = it.project.path,
          config = it.config,
          from = from
        )
      }

    val grouped = redundant
      .groupBy { it.config }
      .mapValues { it.value.toMutableSet() }

    return MCP.Parsed(
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
