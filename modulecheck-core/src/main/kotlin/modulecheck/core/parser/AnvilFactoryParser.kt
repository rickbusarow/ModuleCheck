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

import modulecheck.core.CouldUseAnvilFinding
import modulecheck.core.MCP
import modulecheck.core.files.JavaFile
import modulecheck.core.files.KotlinFile
import net.swiftzer.semver.SemVer
import kotlin.LazyThreadSafetyMode.NONE

object AnvilFactoryParser {

  fun parseLazy(mcp: MCP): Lazy<List<CouldUseAnvilFinding>> = lazy {
    parse(mcp)
  }

  private const val anvilPluginGroupName = "com.squareup.anvil"
  private const val anvilMergeComponent = "com.squareup.anvil.annotations.MergeComponent"
  private const val daggerComponent = "dagger.Component"
  private const val daggerInject = "dagger.Inject"
  private const val daggerModule = "dagger.Module"

  @Suppress("MagicNumber")
  private val minimumAnvilVersion = SemVer(2, 0, 11)

  @Suppress("ComplexMethod")
  fun parse(mcp: MCP): List<CouldUseAnvilFinding> {
    val project = mcp.project

    val anvilVersion = project
      .compilerPluginVersionForGroupName(anvilPluginGroupName)
      ?: return emptyList()

    val hasAnvil = anvilVersion >= minimumAnvilVersion

    if (!hasAnvil) return emptyList()

    val allImports = mcp.mainImports + mcp.androidTestImports + mcp.testImports

    val maybeExtra by lazy(NONE) {
      mcp.androidTestExtraPossibleReferences +
        mcp.mainExtraPossibleReferences +
        mcp.testExtraPossibleReferences
    }

    val createsComponent = allImports.contains(daggerComponent) ||
      allImports.contains(anvilMergeComponent) ||
      maybeExtra.contains(daggerComponent) ||
      maybeExtra.contains(anvilMergeComponent)

    if (createsComponent) return emptyList()

    val usesDaggerInJava = mcp.mainFiles
      .filterIsInstance<JavaFile>()
      .any { file ->
        file.imports.contains(daggerInject) ||
          file.imports.contains(daggerModule) ||
          file.maybeExtraReferences.contains(daggerInject) ||
          file.maybeExtraReferences.contains(daggerModule)
      }

    if (usesDaggerInJava) return emptyList()

    val usesDaggerInKotlin = mcp.mainFiles
      .filterIsInstance<KotlinFile>()
      .any { file ->
        file.imports.contains(daggerInject) ||
          file.imports.contains(daggerModule) ||
          file.maybeExtraReferences.contains(daggerInject) ||
          file.maybeExtraReferences.contains(daggerModule)
      }

    if (!usesDaggerInKotlin) return emptyList()
    if (project.anvilGenerateFactoriesEnabled()) return emptyList()

    val couldBeAnvil =
      !allImports.contains(daggerComponent) && !maybeExtra.contains(daggerComponent)

    return if (couldBeAnvil) {
      listOf(CouldUseAnvilFinding(mcp.project))
    } else {
      listOf()
    }
  }
}
