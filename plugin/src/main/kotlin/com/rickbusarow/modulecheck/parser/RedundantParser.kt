/*
 * Copyright (C) 2020 Rick Busarow
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

package com.rickbusarow.modulecheck.parser

import com.rickbusarow.modulecheck.Config
import com.rickbusarow.modulecheck.DependencyFinding
import com.rickbusarow.modulecheck.MCP
import com.rickbusarow.modulecheck.mcp

object RedundantParser : Parser<DependencyFinding.RedundantDependency>() {

  override fun parse(mcp: MCP): MCP.Parsed<DependencyFinding.RedundantDependency> {

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

        DependencyFinding.RedundantDependency(
          mcp.project,
          it.project,
          it.project.path,
          it.config,
          from
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
