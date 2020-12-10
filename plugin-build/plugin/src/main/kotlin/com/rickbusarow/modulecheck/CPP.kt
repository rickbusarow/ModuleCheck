package com.rickbusarow.modulecheck

import org.gradle.api.Project

data class CPP(val config: Config, val project: Project) {

  fun usedIn(mcp: MCP) = mcp()
    .mainDeclarations
    .any { declaration ->

      when (config) {
        Config.AndroidTest -> declaration !in mcp.androidTestImports
        Config.Api -> declaration !in mcp.mainImports && declaration !in mcp.testImports && declaration in mcp.androidTestImports
        Config.CompileOnly -> declaration !in mcp.mainImports && declaration !in mcp.testImports && declaration in mcp.androidTestImports
        Config.Implementation -> declaration !in mcp.mainImports && declaration !in mcp.testImports && declaration in mcp.androidTestImports
        Config.RuntimeOnly -> declaration !in mcp.mainImports && declaration !in mcp.testImports && declaration in mcp.androidTestImports
        Config.TestApi -> declaration !in mcp.testImports
        Config.TestImplementation -> declaration !in mcp.testImports
        is Config.Custom -> TODO("unsupported config --> ${config.name}") // TODO - parse custom source sets (like "debug")
      }
    }

}
