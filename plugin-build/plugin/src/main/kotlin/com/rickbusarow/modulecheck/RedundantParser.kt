package com.rickbusarow.modulecheck

object RedundantParser {

  fun parse(mcp: MCP)  {

    val allMain = mcp.dependencies.api .toSet()

    val inheritedDependencyProjects = mcp.dependencies.api
      .mcp()
      .flatMap {
        it.allPublicClassPathDependencyDeclarations()
          .map { it.project }
          .toSet()
      } + mcp.dependencies.main ()
      .flatMap {
        it.mcp().allPublicClassPathDependencyDeclarations()
          .map { it.project }
          .toSet()
      }


    return allMain
      .filter { it.project in inheritedDependencyProjects }
      .map {

        val from = allMain
          .filter { inherited -> inherited.project == it.project }
          .map { it.project }

        DependencyFinding.RedundantDependency(
          project.project,
          it.project,
          it.project.path,
          "api",
          from
        )
      }
  }
}
