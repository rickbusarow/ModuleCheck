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

package com.rickbusarow.modulecheck.rule.android

import com.android.build.gradle.LibraryExtension
import com.rickbusarow.modulecheck.Finding
import com.rickbusarow.modulecheck.Fixable
import com.rickbusarow.modulecheck.Position
import com.rickbusarow.modulecheck.internal.asKtFile
import com.rickbusarow.modulecheck.parser.DslBlockParser
import com.rickbusarow.modulecheck.rule.AbstractRule
import net.swiftzer.semver.SemVer
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType

internal val androidBlockParser = DslBlockParser("android")
internal val androidBlockRegex = "^android \\{".toRegex()

data class UnusedResourcesGenerationFinding(
  override val dependentProject: Project
) : Finding, Fixable {

  override val problemName = "unused R file generation"

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

private val MINIMUM_ANDROID_RESOURCES_VERSION = SemVer(major = 4, minor = 1, patch = 0)

class DisableAndroidResourcesRule(
  project: Project,
  alwaysIgnore: Set<String>,
  ignoreAll: Set<String>
) : AbstractRule<UnusedResourcesGenerationFinding>(
  project, alwaysIgnore, ignoreAll
) {

  @Suppress("ReturnCount")
  override fun check(): List<UnusedResourcesGenerationFinding> {
    val libraryExtension = project.extensions.findByType<LibraryExtension>()

    libraryExtension ?: return emptyList()

    // grabs the AGP version of the client project - not this plugin
    val agpVersion = SemVer.parse(com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION)

    // minimum AGP version for this feature is 4.1.0, so don't bother checking below that
    if (agpVersion < MINIMUM_ANDROID_RESOURCES_VERSION) return emptyList()

    @Suppress("UnstableApiUsage")
    if (libraryExtension.buildFeatures.androidResources == false) return emptyList()

    val noResources = libraryExtension.sourceSets.all { sourceSet ->
      sourceSet.res.getSourceFiles().isEmpty
    }

    return if (noResources) {
      listOf(UnusedResourcesGenerationFinding(project))
    } else {
      emptyList()
    }
  }
}
