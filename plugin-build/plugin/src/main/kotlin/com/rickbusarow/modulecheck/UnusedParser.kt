package com.rickbusarow.modulecheck

object UnusedParser {

  fun parse(mcp: MCP): MCP.Parsed<DependencyFinding.UnusedDependency> {

    val dependencies = mcp.dependencies

    val unusedHere = dependencies
      .all()
      .filter { cpp ->
        !cpp.usedIn(mcp)
      }

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

      DependencyFinding.UnusedDependency(
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
