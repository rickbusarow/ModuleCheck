package com.rickbusarow.modulecheck.parser

import com.rickbusarow.modulecheck.MCP
import org.gradle.api.artifacts.Dependency

object KaptParser {

  fun parseLazy(mcp: MCP): Lazy<MCP.ParsedKapt<KaptProcessor>> = lazy {
    parse(mcp)
  }

  fun parse(mcp: MCP): MCP.ParsedKapt<KaptProcessor> {
    val grouped = mcp.project
      .configurations
      .groupBy { it.name }
      .mapValues { (_, configurations) ->
        configurations.flatMap { config ->
          config
            .dependencies
            .map { dep: Dependency ->

              val comb = dep.group + ":" + dep.name

              KaptProcessor(comb)
            }
        }.toSet()
      }

    return MCP.ParsedKapt(
      grouped.getOrDefault("kaptAndroidTest", setOf()),
      grouped.getOrDefault("kapt", setOf()),
      grouped.getOrDefault("kaptTest", setOf())
    )
  }
}
