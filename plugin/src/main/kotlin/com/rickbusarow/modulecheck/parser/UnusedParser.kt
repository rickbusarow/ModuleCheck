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

package com.rickbusarow.modulecheck.parser

import com.rickbusarow.modulecheck.Config
import com.rickbusarow.modulecheck.MCP
import com.rickbusarow.modulecheck.UnusedDependency

fun <E> Collection<E>.lines() = joinToString("\n")

object UnusedParser : Parser<UnusedDependency>() {

  override fun parse(mcp: MCP): MCP.Parsed<UnusedDependency> {
    fun log(msg: () -> String) {
      if (mcp.project.path == ":kits:data") {
        println(msg())
      }
    }

    val dependencies = mcp.dependencies

    val unusedHere = dependencies
      .all()
      .filter { cpp -> !cpp.usedIn(mcp) }

    log {
      """ ********************************************************
      |
      |unused here
      |
      |${unusedHere.lines()}
      |
      |_______________________________________________________
    """.trimMargin()
    }

    val dependents = mcp.dependents()

    log {
      """ ********************************************************
      |
      |dependents
      |
      |${dependents.lines()}
      |
      |_______________________________________________________
    """.trimMargin()
    }

    val unusedMain = dependencies
      .main()
      .filter { it !in mcp.resolvedMainDependencies }

    log {
      """ ********************************************************
      |
      |unused main
      |
      |${unusedMain.lines()}
      |
      |_______________________________________________________
    """.trimMargin()
    }

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

    log {
      """ ********************************************************
      |
      |unusedInAtLeastOneDependent
      |
      |${unusedInAtLeastOneDependent.lines()}
      |
      |_______________________________________________________
    """.trimMargin()
    }

    val grouped = unusedInAtLeastOneDependent.map { cpp ->

      UnusedDependency(
        mcp.project,
        cpp.project,
        cpp.project.path,
        cpp.config
      )
    }
      .groupBy { it.config }
      .mapValues { it.value.toMutableSet() }

    log {
      """ ********************************************************
      |
      |grouped
      |
      |${grouped.entries.lines()}
      |
      |_______________________________________________________
    """.trimMargin()
    }

    val newGrouped = unusedHere.map { cpp ->

      UnusedDependency(
        mcp.project,
        cpp.project,
        cpp.project.path,
        cpp.config
      )
    }
      .groupBy { it.config }
      .mapValues { it.value.toMutableSet() }

    log {
      """ ********************************************************
      |
      |newGrouped
      |
      |${newGrouped.entries.lines()}
      |
      |_______________________________________________________
    """.trimMargin()
    }

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
