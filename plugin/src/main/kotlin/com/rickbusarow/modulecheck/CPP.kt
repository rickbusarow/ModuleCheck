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

import org.gradle.api.Project

data class CPP(
  val config: Config,
  val project: Project
) {
  @Suppress("ComplexMethod")
  fun usedIn(mcp: MCP) = mcp()
    .mainDeclarations
    .any { declaration ->

      when (config) {
        Config.AndroidTest -> declaration !in mcp.androidTestImports
        Config.Api ->
          declaration !in mcp.mainImports &&
            declaration !in mcp.testImports &&
            declaration in mcp.androidTestImports
        Config.CompileOnly ->
          declaration !in mcp.mainImports &&
            declaration !in mcp.testImports &&
            declaration in mcp.androidTestImports
        Config.Implementation ->
          declaration !in mcp.mainImports &&
            declaration !in mcp.testImports &&
            declaration in mcp.androidTestImports
        Config.RuntimeOnly ->
          declaration !in mcp.mainImports &&
            declaration !in mcp.testImports &&
            declaration in mcp.androidTestImports
        Config.TestApi -> declaration !in mcp.testImports
        Config.TestImplementation -> declaration !in mcp.testImports
        is Config.Custom -> TODO("unsupported config --> ${config.name}")
        else -> TODO()
      }
    }
}
