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

package modulecheck.core.rule.android

import modulecheck.*
import modulecheck.api.*
import modulecheck.api.Finding.Position
import modulecheck.core.internal.asKtFile
import modulecheck.core.rule.AbstractRule
import net.swiftzer.semver.SemVer

data class UnusedViewBindingGenerationFinding(
  override val dependentProject: Project2
) : Finding, Fixable {

  override val problemName = "unused ViewBinding generation"

  override val dependencyIdentifier = ""

  override fun positionOrNull(): Position? {
    val ktFile = dependentProject.buildFile.asKtFile()

    return androidBlockParser.parse(ktFile)?.let { result ->

      val token = result
        .blockText
        .lines()
        .firstOrNull { it.isNotEmpty() } ?: return@let null

      val lines = ktFile.text.lines()

      val startRow = lines.indexOfFirst { it.matches(androidBlockRegex) }

      if (startRow == -1) return@let null

      val after = lines.subList(startRow, lines.lastIndex)

      val row = after.indexOfFirst { it.contains(token) }

      Position(row + startRow + 1, 0)
    }
  }

  override fun fix(): Boolean {
    val ktFile = dependentProject.buildFile.asKtFile()

    return false
  }
}

private val MINIMUM_ANDROID_RESOURCES_VERSION = SemVer(major = 4, minor = 0, patch = 0)

class DisableViewBindingRule(
  project: AndroidProject2,
  alwaysIgnore: Set<String>,
  ignoreAll: Set<String>
) : AbstractRule<UnusedViewBindingGenerationFinding>(
  project, alwaysIgnore, ignoreAll
) {

  @Suppress("ReturnCount")
  override fun check(): List<UnusedViewBindingGenerationFinding> {
    val androidProject = project as? AndroidProject2 ?: return emptyList()

    // grabs the AGP version of the client project - not this plugin
    val agpVersion = androidProject.agpVersion

    // minimum AGP version for this feature is 4.0.0, so don't bother checking below that
    if (agpVersion < MINIMUM_ANDROID_RESOURCES_VERSION) return emptyList()

    // no chance of a finding if the feature's already disabled
    @Suppress("UnstableApiUsage")
    if (!androidProject.viewBindingEnabled) return emptyList()

    val filterReg = """layout*/*.xml""".toRegex()

    val noLayouts = androidProject
      .resourceFiles
      .filter { it.path.matches(filterReg) }
      .any { it.exists() }

    return if (noLayouts) {
      listOf(UnusedViewBindingGenerationFinding(project))
    } else {
      emptyList()
    }
  }
}
