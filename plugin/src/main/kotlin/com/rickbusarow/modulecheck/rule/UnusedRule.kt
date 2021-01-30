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

package com.rickbusarow.modulecheck.rule

import com.rickbusarow.modulecheck.UnusedDependency
import com.rickbusarow.modulecheck.mcp
import org.gradle.api.Project

class UnusedRule(
  project: Project,
  alwaysIgnore: Set<String>,
  ignoreAll: Set<String>
) : AbstractRule<UnusedDependency>(
  project, alwaysIgnore, ignoreAll
) {

  override fun check(): List<UnusedDependency> {

    if (project.path in ignoreAll) return emptyList()

    return project.mcp()
      .unused
      .all()
      .mapNotNull { dependency ->
        if (dependency.dependencyIdentifier in alwaysIgnore) {
          null
        } else {
          dependency
        }
      }
      .distinctBy { it.position() }
  }
}
