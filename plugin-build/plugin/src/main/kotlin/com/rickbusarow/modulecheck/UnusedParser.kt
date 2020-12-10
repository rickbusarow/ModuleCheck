package com.rickbusarow.modulecheck

object UnusedParser {

  fun parse(mcp: MCP): MCP.Parsed<DependencyFinding2.UnusedDependency> {

    val dependencies = mcp.dependencies

    val unusedHere = dependencies
      .all()
      .filter { cpp ->
        !cpp.usedIn(mcp)
      }

    val dependents = mcp.dependents()

    val unusedAnywhere = unusedHere
      .filter { cpp ->
        cpp.config != Config.Api || dependents.all { dependent ->
          !cpp.usedIn(dependent)
        }
      }

    val grouped = unusedAnywhere.map { cpp ->

      DependencyFinding2.UnusedDependency(
        mcp.project,
        cpp.project,
        cpp.project.path,
        cpp.config
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
