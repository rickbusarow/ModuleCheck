/*
 * Copyright (C) 2021-2023 Rick Busarow
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

package modulecheck.parsing.gradle.dsl.internal

import modulecheck.finding.FindingName
import modulecheck.parsing.gradle.dsl.PluginDeclaration
import modulecheck.parsing.gradle.dsl.PluginsBlock
import modulecheck.reporting.logging.McLogger
import modulecheck.utils.lazy.ResetManager
import modulecheck.utils.lazy.lazyResets
import java.io.File

abstract class AbstractPluginsBlock(
  private val logger: McLogger,
  blockSuppressed: List<String>
) : PluginsBlock {

  private val resetManager = ResetManager()

  protected val originalLines: MutableList<String> by lazy { lambdaContent.lines().toMutableList() }

  private val _allDeclarations = mutableListOf<PluginDeclaration>()

  override val settings: List<PluginDeclaration>
    get() = _allDeclarations

  override val blockSuppressed: List<String> = blockSuppressed.updateOldSuppresses()

  override val allSuppressions: Map<PluginDeclaration, Set<FindingName>> by resetManager.lazyResets {
    buildMap<PluginDeclaration, MutableSet<FindingName>> {

      _allDeclarations.forEach { pluginDeclaration ->

        val cached = getOrPut(pluginDeclaration) {
          blockSuppressed.mapNotNullTo(mutableSetOf()) { FindingName.safe(it) }
        }

        cached += pluginDeclaration.suppressed.updateOldSuppresses()
          .plus(blockSuppressed)
          .asFindingNames()
      }
    }
  }

  fun addStatement(parsedString: String, suppressed: List<String>) {
    val originalString = getOriginalString(parsedString)

    val declaration = PluginDeclaration(
      statementWithSurroundingText = originalString,
      declarationText = parsedString,
      suppressed = suppressed.updateOldSuppresses() + blockSuppressed
    )
    _allDeclarations.add(declaration)
    resetManager.reset()
  }

  protected abstract fun findOriginalStringIndex(parsedString: String): Int

  override fun getById(pluginId: String): PluginDeclaration? {
    val regex = pluginId.let { Regex.escape(it) }
      .replace("\\.", "\\s*\\.\\s*")
      .toRegex()

    return settings.firstOrNull { it.declarationText.contains(regex) }
  }

  private fun getOriginalString(parsedString: String): String {
    val originalStringIndex = findOriginalStringIndex(parsedString)

    val originalStringLines = List(originalStringIndex + 1) {
      originalLines.removeFirst()
    }

    return originalStringLines.joinToString("\n")
  }

  private fun List<String>.updateOldSuppresses(): List<String> {
    @Suppress("DEPRECATION")
    return map { originalName ->
      FindingName.migrateLegacyIdOrNull(originalName, logger) ?: originalName
    }
  }

  private fun Collection<String>.asFindingNames(): Set<FindingName> {
    return mapNotNullTo(mutableSetOf()) { FindingName.safe(it) }
  }
}

interface PluginsBlockProvider {

  fun get(): PluginsBlock?

  fun interface Factory {
    fun create(buildFile: File): PluginsBlockProvider
  }
}
