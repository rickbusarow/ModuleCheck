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

package modulecheck.core.internal

import modulecheck.api.Config
import modulecheck.api.ConfiguredProjectDependency
import modulecheck.core.MCP
import kotlin.LazyThreadSafetyMode.NONE

@Suppress("ComplexMethod")
fun ConfiguredProjectDependency.usedIn(mcp: MCP): Boolean {
  return when (config) {
    Config.AndroidTest -> usedInAndroidTest(mcp)
    // Config.KaptAndroidTest -> TODO()
    // Config.Kapt -> TODO()
    // Config.KaptTest -> TODO()
    Config.Api -> usedInMain(mcp)
    Config.CompileOnly -> usedInMain(mcp)
    Config.Implementation -> usedInMain(mcp)
    Config.RuntimeOnly -> usedInMain(mcp)
    Config.TestApi -> usedInTest(mcp)
    Config.TestImplementation -> usedInTest(mcp)
    is Config.Custom -> TODO("unsupported config --> ${config.name}")
    else -> TODO()
  }
}

fun ConfiguredProjectDependency.mcp() = MCP.from(project)

private fun ConfiguredProjectDependency.usedInMain(mcp: MCP): Boolean {
  val thisAsMCP = mcp()

  val javaIsUsed = thisAsMCP
    .mainDeclarations
    .any { declaration ->

      declaration in mcp.mainImports || declaration in mcp.mainExtraPossibleReferences
    }

  if (javaIsUsed) return true

  val rReferences = mcp.mainExtraPossibleReferences.filter { it.startsWith("R.") }

  val resourcesAreUsed = thisAsMCP
    .mainAndroidResDeclarations
    .any { rDeclaration ->
      rDeclaration in rReferences
    }

  return resourcesAreUsed
}

private fun ConfiguredProjectDependency.usedInAndroidTest(mcp: MCP): Boolean {
  val rReferences by lazy(NONE) { mcp.androidTestExtraPossibleReferences.filter { it.startsWith("R.") } }

  return mcp()
    .mainDeclarations
    .any { declaration ->
      declaration in mcp.androidTestImports || declaration in mcp.androidTestExtraPossibleReferences
    } || mcp()
    .mainAndroidResDeclarations
    .any { rDeclaration ->
      rDeclaration in rReferences
    }
}

private fun ConfiguredProjectDependency.usedInTest(mcp: MCP): Boolean {
  val rReferences by lazy(NONE) { mcp.testExtraPossibleReferences.filter { it.startsWith("R.") } }

  return mcp()
    .mainDeclarations
    .any { declaration ->
      declaration in mcp.testImports || declaration in mcp.testExtraPossibleReferences
    } || mcp()
    .mainAndroidResDeclarations
    .any { rDeclaration ->
      rDeclaration in rReferences
    }
}
