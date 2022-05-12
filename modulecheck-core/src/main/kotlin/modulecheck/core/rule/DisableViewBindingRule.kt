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

import modulecheck.api.context.androidDataBindingDeclarationsForSourceSetName
import modulecheck.api.context.dependents
import modulecheck.api.context.referencesForSourceSetName
import modulecheck.config.ChecksSettings
import modulecheck.core.rule.android.DisableViewBindingGenerationFinding
import modulecheck.finding.FindingName
import modulecheck.project.McProject
import modulecheck.project.project
import modulecheck.utils.any
import modulecheck.utils.lazyDeferred

class DisableViewBindingRule : DocumentedRule<DisableViewBindingGenerationFinding>() {

  override val name = FindingName("disable-view-binding")
  override val description = "Finds modules which have ViewBinding enabled, " +
    "but don't actually use any generated ViewBinding objects from that module"

  @Suppress("ReturnCount")
  override suspend fun check(project: McProject): List<DisableViewBindingGenerationFinding> {
    val androidPlugin = project.platformPlugin.asAndroidOrNull() ?: return emptyList()

    // no chance of a finding if the feature's already disabled
    @Suppress("UnstableApiUsage")
    if (!androidPlugin.viewBindingEnabled) return emptyList()

    val dependents = lazyDeferred { project.dependents() }

    androidPlugin.sourceSets.keys
      .forEach { sourceSetName ->

        val generatedBindings = project
          .androidDataBindingDeclarationsForSourceSetName(sourceSetName)
          .takeIf { it.isNotEmpty() }
          ?: return@forEach

        val usedInProject = sourceSetName.withDownStream(project)
          .any { sourceSetNameOrDownstream ->

            generatedBindings.any { generated ->

              project.referencesForSourceSetName(sourceSetNameOrDownstream)
                .contains(generated)
            }
          }

        if (usedInProject) return emptyList()

        val usedInDependent = dependents
          .await()
          .any { downstream ->

            // Get the source set which is exposed to the dependent project through its
            // configuration.  This will typically be `main`, but it could be another build variant
            // if the entire dependency chain is using another source set's configuration.
            val exposedSourceSetName = downstream.configuredProjectDependency
              .declaringSourceSetName()

            val references = downstream.project(project.projectCache)
              .referencesForSourceSetName(exposedSourceSetName)

            generatedBindings.any { generated ->
              references.contains(generated)
            }
          }

        if (usedInDependent) return emptyList()
      }

    return listOf(
      DisableViewBindingGenerationFinding(
        findingName = name,
        dependentProject = project,
        dependentPath = project.path,
        buildFile = project.buildFile
      )
    )
  }

  override fun shouldApply(checksSettings: ChecksSettings): Boolean {
    return checksSettings.disableViewBinding
  }
}
