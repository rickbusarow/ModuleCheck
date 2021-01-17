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
import com.rickbusarow.modulecheck.MCP
import com.rickbusarow.modulecheck.OverShotDependency
import com.rickbusarow.modulecheck.mcp

object OvershotParser : Parser<OverShotDependency>() {

  override fun parse(mcp: MCP): MCP.Parsed<OverShotDependency> {
    val unused = mcp.unused
      .main()
      .map { it.cpp() }
      .toSet()

    val apiFromUnused = unused
      .flatMap { cpp ->
        cpp
          .mcp()
          .dependencies.api
      }.toSet()

    val unusedPaths = unused
      .map { it.project.path }
      .toSet()

    val mainDependenciesPaths = mcp.dependencies
      .main()
      .map { it.project.path }
      .toSet()

    val grouped = apiFromUnused
      .asSequence()
      .filterNot { it.project.path in unusedPaths }
      .filterNot { it.project.path in mainDependenciesPaths }
      .filter { inheritedDependencyProject ->
        inheritedDependencyProject
          .mcp()
          .mainDeclarations
          .any { newProjectDeclaration ->
            newProjectDeclaration in mcp.mainImports
          }
      }
      .distinct()
      .map { overshot ->

        val source = mcp.sourceOf(overshot)
        val sourceConfig = mcp
          .dependencies
          .main()
          .firstOrNull { it.project == source?.project }
          ?.config

        OverShotDependency(
          dependentProject = mcp.project,
          dependencyProject = overshot.project,
          dependencyPath = overshot.project.path,
          config = sourceConfig ?: Config.Api,
          from = source
        )
      }
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
