package com.rickbusarow.modulecheck.parser

import org.gradle.api.artifacts.ProjectDependency
import com.rickbusarow.modulecheck.*

object DependencyParser : Parser<CPP>() {

  override fun parse(mcp: MCP): MCP.Parsed<CPP> {

    val grouped = mcp.project
      .configurations
      .groupBy { it.name }
      .mapValues { (_, configurations) ->
        configurations.flatMap { config ->
          config
            .dependencies.withType(ProjectDependency::class.java)
            .map { CPP(Config.from(config.name), it.dependencyProject) }
        }.toMutableSet()
      }

    return MCP.Parsed(
      grouped.getOrDefault("androidTest", mutableSetOf()),
      grouped.getOrDefault("api", mutableSetOf()),
      grouped.getOrDefault("compileOnly", mutableSetOf()),
      grouped.getOrDefault("implementation", mutableSetOf()),
      grouped.getOrDefault("runtimeOnly", mutableSetOf()),
      grouped.getOrDefault("testApi", mutableSetOf()),
      grouped.getOrDefault("testImplementation", mutableSetOf())
    )

  }

}

