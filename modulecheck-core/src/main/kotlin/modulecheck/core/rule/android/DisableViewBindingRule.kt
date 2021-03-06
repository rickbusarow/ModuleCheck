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

import modulecheck.api.AndroidProject2
import modulecheck.api.Project2
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.core.rule.ModuleCheckRule
import net.swiftzer.semver.SemVer

private val MINIMUM_ANDROID_RESOURCES_VERSION = SemVer(major = 4, minor = 0, patch = 0)

class DisableViewBindingRule(
  override val settings: ModuleCheckSettings
) : ModuleCheckRule<DisableViewBindingGenerationFinding>() {

  override val id = "DisableViewBinding"
  override val description = "Finds modules which have ViewBinding enabled, " +
    "but don't actually use any generated ViewBinding objects from that module"

  @Suppress("ReturnCount")
  override fun check(project: Project2): List<DisableViewBindingGenerationFinding> {
    val androidProject = project as? AndroidProject2 ?: return emptyList()

    // grabs the AGP version of the client project - not this plugin
    val agpVersion = androidProject.agpVersion

    // minimum AGP version for this feature is 4.0.0, so don't bother checking below that
    if (agpVersion < MINIMUM_ANDROID_RESOURCES_VERSION) return emptyList()

    // no chance of a finding if the feature's already disabled
    @Suppress("UnstableApiUsage")
    if (!androidProject.viewBindingEnabled) return emptyList()

    val layouts = androidProject
      .resourceFiles
      .filter { it.path.matches(filterReg) }

    val dependents = project.dependendents

    val basePackage = project.androidPackageOrNull
      ?: return listOf(DisableViewBindingGenerationFinding(project.path, project.buildFile))

    val usedLayouts = layouts
      .filter { it.exists() }
      .filter { file ->

        val generated = file
          .nameWithoutExtension
          .split("_")
          .joinToString("") { fragment -> fragment.capitalize() } + "Binding"

        val reference = "$basePackage.databinding.$generated"

        dependents
          .any { dep ->

            project
              .importsForSourceSetName("main")
              .contains(reference) ||
              dep
                .extraPossibleReferencesForSourceSetName("main")
                .contains(reference)
          }
      }
      .toList()

    return if (usedLayouts.isNotEmpty()) {
      emptyList()
    } else {
      listOf(DisableViewBindingGenerationFinding(project.path, project.buildFile))
    }
  }

  companion object {
    private val filterReg = """.*/layout.*/.*.xml""".toRegex()
  }
}
