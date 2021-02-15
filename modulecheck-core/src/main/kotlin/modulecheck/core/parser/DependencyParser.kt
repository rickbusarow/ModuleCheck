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

package modulecheck.core.parser

import modulecheck.api.ConfiguredProjectDependency
import modulecheck.api.Project2
import modulecheck.core.MCP

object DependencyParser : Parser<ConfiguredProjectDependency>() {

  override fun parse(project: Project2): MCP.Parsed<ConfiguredProjectDependency> {
    val grouped = project
      .projectDependencies
      .groupBy { it.config.name }
      .mapValues { (_, lst) -> lst.toSet() }

    return MCP.Parsed(
      grouped.getOrDefault("androidTest", setOf()),
      grouped.getOrDefault("api", setOf()),
      grouped.getOrDefault("compileOnly", setOf()),
      grouped.getOrDefault("implementation", setOf()),
      grouped.getOrDefault("runtimeOnly", setOf()),
      grouped.getOrDefault("testApi", setOf()),
      grouped.getOrDefault("testImplementation", setOf())
    )
  }
}
