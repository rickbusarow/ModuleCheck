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

package com.rickbusarow.modulecheck.testing

import java.nio.file.Path

class ProjectSettingsSpec private constructor(
  val includes: List<String>
) {
  fun writeIn(path: Path) {
    path.toFile().mkdirs()
    path.newFile("settings.gradle.kts").writeText(
      pluginManagement() +  includes()
    )
  }

  private fun pluginManagement() =
    """pluginManagement {
       |  repositories {
       |    gradlePluginPortal()
       |    jcenter()
       |    google()
       |  }
       |  resolutionStrategy {
       |    eachPlugin { 
       |      if (requested.id.id.startsWith("com.android")) {
       |        useModule("com.android.tools.build:gradle:4.1.1")
       |      }
       |      if (requested.id.id.startsWith("org.jetbrains.kotlin")) {
       |        useVersion("1.4.21")
       |      }
       |    }
       |  }
       |}
       |
       |""".trimMargin()

  private fun includes() = includes.joinToString(",\n", "include(\n", "\n)") { "  \":$it\"" }

  class Builder {

    private val includes = mutableListOf<String>()

    fun addInclude(include: String) = apply {
      includes.add(include)
    }

    fun build() = ProjectSettingsSpec(includes)
  }
}
