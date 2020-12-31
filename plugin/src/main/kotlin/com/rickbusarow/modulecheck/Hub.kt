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

package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.internal.Output
import org.gradle.api.Project
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.util.concurrent.ConcurrentHashMap

class MCP2 private constructor(
  val project: Project
) : Comparable<MCP2> {

  init {
    cache[project] = this
  }

  override fun compareTo(other: MCP2): Int = project.path.compareTo(other.project.path)

  companion object {
    private val cache = ConcurrentHashMap<Project, MCP2>()

    fun reset() {
      Output.printGreen("                                                          resetting")
      cache.clear()
    }

    fun from(project: Project): MCP2 = cache.getOrPut(project) { MCP2(project) }
  }
}

class FF {

  @PublishedApi
  internal val _map = ConcurrentHashMap<McpComponent.Key<*>, Any>()

  inline fun <reified T : McpComponent> get(key: McpComponent.Key<T>): T {
    return _map.getOrPut(key) { key.create() }.cast()
  }
}

interface McpComponent {

  interface Key<T : McpComponent> {
    fun create(): T
  }
}
