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

package modulecheck.core.rule

import modulecheck.api.ModuleCheckRule
import modulecheck.api.context.dependendents
import modulecheck.api.context.importsForSourceSetName
import modulecheck.api.context.layoutFiles
import modulecheck.api.context.possibleReferencesForSourceSetName
import modulecheck.api.settings.ChecksSettings
import modulecheck.core.rule.android.DisableViewBindingGenerationFinding
import modulecheck.parsing.AndroidMcProject
import modulecheck.parsing.McProject
import modulecheck.parsing.SourceSetName
import modulecheck.parsing.all
import modulecheck.parsing.capitalize

class DisableViewBindingRule : ModuleCheckRule<DisableViewBindingGenerationFinding> {

  override val id = "DisableViewBinding"
  override val description = "Finds modules which have ViewBinding enabled, " +
    "but don't actually use any generated ViewBinding objects from that module"

  @Suppress("ReturnCount")
  override suspend fun check(project: McProject): List<DisableViewBindingGenerationFinding> {
    val androidProject = project as? AndroidMcProject ?: return emptyList()

    // no chance of a finding if the feature's already disabled
    @Suppress("UnstableApiUsage")
    if (!androidProject.viewBindingEnabled) return emptyList()

    val layouts = androidProject
      .layoutFiles()
      .all()

    val dependents = project.dependendents()

    val basePackage = project.androidPackageOrNull
      ?: return listOf(DisableViewBindingGenerationFinding(project.path, project.buildFile))

    val usedLayouts = layouts
      .filter { it.file.exists() }
      .filter { layoutFile ->

        val generated = layoutFile.file
          .nameWithoutExtension
          .split("_")
          .joinToString("") { fragment -> fragment.capitalize() } + "Binding"

        val reference = "$basePackage.databinding.$generated"

        val usedInProject = project
          .importsForSourceSetName(SourceSetName.MAIN)
          .contains(reference)

        usedInProject || dependents
          .any { dep ->

            dep
              .importsForSourceSetName(SourceSetName.MAIN)
              .contains(reference) || dep
              .possibleReferencesForSourceSetName(SourceSetName.MAIN)
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

  override fun shouldApply(checksSettings: ChecksSettings): Boolean {
    return checksSettings.disableViewBinding
  }
}
