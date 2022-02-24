/*
 * Copyright (C) 2021-2022 Rick Busarow
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

import modulecheck.api.context.androidBasePackagesForSourceSetName
import modulecheck.api.context.androidResourceReferencesForSourceSetName
import modulecheck.api.context.dependents
import modulecheck.api.context.importsForSourceSetName
import modulecheck.api.context.layoutFilesForSourceSetName
import modulecheck.api.rule.ModuleCheckRule
import modulecheck.api.settings.ChecksSettings
import modulecheck.core.rule.android.DisableViewBindingGenerationFinding
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.source.asExplicitReference
import modulecheck.project.AndroidMcProject
import modulecheck.project.McProject
import modulecheck.utils.capitalize
import modulecheck.utils.existsOrNull

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

    val dependents = project.dependents()

    project.sourceSets.keys
      .forEach { sourceSetName ->

        val basePackage = project.androidBasePackagesForSourceSetName(sourceSetName)
          ?: return@forEach

        val generatedBindings = project.layoutFilesForSourceSetName(sourceSetName)
          .mapNotNull { it.file.existsOrNull() }
          .map { layoutFile ->
            val simpleBindingName = layoutFile.nameWithoutExtension
              .split("_")
              .joinToString("") { fragment -> fragment.capitalize() } + "Binding"

            // fully qualified
            "$basePackage.databinding.$simpleBindingName".asExplicitReference()
          }

        val usedInProject = sourceSetName.withDownStream(project)
          .any { sourceSetNameOrDownstream ->

            generatedBindings.any { generated ->

              project.importsForSourceSetName(sourceSetNameOrDownstream)
                .contains(generated)
            }
          }

        if (usedInProject) return emptyList()

        // TODO -- this needs to be changed to respect the source sets of the downstream project
        val usedInDependent = dependents
          .any { dep ->

            generatedBindings.any { generated ->
              dep
                .importsForSourceSetName(SourceSetName.MAIN)
                .contains(generated) || dep
                .androidResourceReferencesForSourceSetName(SourceSetName.MAIN)
                .contains(generated)
            }
          }

        if (usedInDependent) return emptyList()
      }

    return listOf(
      DisableViewBindingGenerationFinding(
        dependentProject = project, dependentPath = project.path, buildFile = project.buildFile
      )
    )
  }

  override fun shouldApply(checksSettings: ChecksSettings): Boolean {
    return checksSettings.disableViewBinding
  }
}
