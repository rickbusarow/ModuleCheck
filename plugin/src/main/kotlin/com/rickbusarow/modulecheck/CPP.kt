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

package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.Config.*
import org.gradle.api.Project

data class CPP(
  val config: Config,
  val project: Project
) {
  @Suppress("ComplexMethod")
  fun usedIn(mcp: MCP): Boolean {
/*
    if (project.path == ":places:data" && mcp.project.path == ":kits:data") {
      println(
        """---------------------------------------------------------------------
        |
        | -- places data declarations
        |
        | ${mcp().mainDeclarations.joinToString("\n")}
        |
        | -- kits data possible references
        |
        | ${mcp.mainExtraPossibleReferences.joinToString("\n")}
        |
        | -- kits data imports
        |
        | ${mcp.mainImports.joinToString("\n")}
        |
        | ======================================================================
      """.trimMargin()
      )
    }
*/

    return when (config) {
      AndroidTest -> usedInAndroidTest(mcp)
      // KaptAndroidTest -> TODO()
      // Kapt -> TODO()
      // KaptTest -> TODO()
      Api -> usedInMain(mcp)
      CompileOnly -> usedInMain(mcp)
      Implementation -> usedInMain(mcp)
      RuntimeOnly -> usedInMain(mcp)
      TestApi -> usedInTest(mcp)
      TestImplementation -> usedInTest(mcp)
      is Config.Custom -> TODO("unsupported config --> ${config.name}")
      else -> TODO()
    }
  }

  private fun usedInMain(mcp: MCP): Boolean {
    return mcp()
      .mainDeclarations
      .any { declaration ->

        declaration in mcp.mainImports ||
          declaration in mcp.mainExtraPossibleReferences ||
          declaration in mcp.testImports ||
          declaration in mcp.androidTestImports
      }
  }

  private fun usedInAndroidTest(mcp: MCP): Boolean {
    return mcp()
      .androidTestDeclarations
      .any { declaration ->
        declaration in mcp.androidTestImports
      }
  }

  private fun usedInTest(mcp: MCP): Boolean {
    return mcp()
      .testDeclarations
      .any { declaration ->
        declaration in mcp.testImports
      }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CPP) return false

    if (config != other.config) return false
    if (project.path != other.project.path) return false

    return true
  }

  override fun hashCode(): Int {
    var result = config.hashCode()
    result = 31 * result + project.hashCode()
    return result
  }
}
