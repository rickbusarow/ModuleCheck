package com.rickbusarow.modulecheck

object RedundantParser {

  fun parse(mcp: MCP): MCP.Parsed<DependencyFinding.RedundantDependency> {

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
