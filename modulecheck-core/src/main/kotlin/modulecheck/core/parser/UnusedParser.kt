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
import modulecheck.core.UnusedDependency
import modulecheck.core.internal.usedIn
import modulecheck.core.mcp

fun <E> Collection<E>.lines() = joinToString("\n")

object UnusedParser : Parser<UnusedDependency>() {

  override fun parse(project: Project2): MCP.Parsed<UnusedDependency> {
    val mcp = project.mcp()

    val dependencies = mcp.dependencies

    val unusedHere = dependencies
      .all()
      .filter { cpp -> !cpp.usedIn(mcp) }

    val dependents = mcp.dependents()

    /*
    If a module doesn't use a dependency,
    but it's an api dependency,
    and ALL dependents of that module use it,
    then ignore the fact that it's unused in the current module.
     */
    val unusedInAtLeastOneDependent = unusedHere
      .filter { cpp ->
        cpp.config != Config.Api || dependents.any { dependent ->
          !cpp.usedIn(dependent)
        }
      }

    val grouped = unusedInAtLeastOneDependent.map { cpp ->

      UnusedDependency(
        mcp.project.buildFile,
        cpp.project,
        cpp.project.path,
        cpp.config
      )
    }
      .groupBy { it.config }
      .mapValues { it.value.toMutableSet() }

    val newGrouped = unusedHere.map { cpp ->

      UnusedDependency(
        mcp.project.buildFile,
        cpp.project,
        cpp.project.path,
        cpp.config
      )
    }
      .groupBy { it.config }
      .mapValues { it.value.toMutableSet() }

    return MCP.Parsed(
      grouped.getOrDefault(Config.AndroidTest, mutableSetOf()),
      newGrouped.getOrDefault(Config.Api, mutableSetOf()),
      newGrouped.getOrDefault(Config.CompileOnly, mutableSetOf()),
      newGrouped.getOrDefault(Config.Implementation, mutableSetOf()),
      newGrouped.getOrDefault(Config.RuntimeOnly, mutableSetOf()),
      grouped.getOrDefault(Config.TestApi, mutableSetOf()),
      grouped.getOrDefault(Config.TestImplementation, mutableSetOf())
    )
  }
}
