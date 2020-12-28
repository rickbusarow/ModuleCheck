/*
 * Copyright (C) 2020 Rick Busarow
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

import com.rickbusarow.modulecheck.CPP
import com.rickbusarow.modulecheck.Config
import com.rickbusarow.modulecheck.MCP
import org.gradle.api.artifacts.ProjectDependency

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
