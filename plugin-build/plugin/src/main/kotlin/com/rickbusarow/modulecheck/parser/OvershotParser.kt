package com.rickbusarow.modulecheck.parser

import com.rickbusarow.modulecheck.Config
import com.rickbusarow.modulecheck.DependencyFinding
import com.rickbusarow.modulecheck.MCP
import com.rickbusarow.modulecheck.mcp

object OvershotParser : Parser<DependencyFinding.OverShotDependency>() {

  override fun parse(mcp: MCP): MCP.Parsed<DependencyFinding.OverShotDependency> {

    val bad = (mcp.unused.main())
      .map { it.cpp() }
      .toSet()

    val grouped = mcp.allPublicClassPathDependencyDeclarations()
      .asSequence()
      .filterNot { it in bad }
      .filterNot { it in mcp.dependencies.api }
      .filter { inheritedNewProject ->
        inheritedNewProject.mcp().mainDeclarations.any { newProjectPackage ->
          newProjectPackage in mcp.mainImports
        }
      }
      .groupBy { it }
      .map { (overshot, _) ->

        val source = mcp.sourceOf(overshot)

        DependencyFinding.OverShotDependency(
          mcp.project,
          overshot.project,
          overshot.project.path,
          Config.Api,
          source
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
