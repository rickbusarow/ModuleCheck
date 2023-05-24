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

package modulecheck.rule.impl

import modulecheck.api.context.androidDataBindingDeclarationsForSourceSetName
import modulecheck.api.context.androidRDeclaredNameForSourceSetName
import modulecheck.api.context.androidResourceDeclaredNamesForSourceSetName
import modulecheck.api.context.androidResourceReferencesForSourceSetName
import modulecheck.api.context.dependents
import modulecheck.api.context.referencesForSourceSetName
import modulecheck.config.ModuleCheckSettings
import modulecheck.finding.FindingName
import modulecheck.finding.android.UnusedResourcesGenerationFinding
import modulecheck.model.dependency.AndroidPlatformPlugin.AndroidLibraryPlugin
import modulecheck.project.McProject
import modulecheck.project.project
import javax.inject.Inject

class DisableAndroidResourcesRule @Inject constructor() :
  DocumentedRule<UnusedResourcesGenerationFinding>() {

  override val name: FindingName = UnusedResourcesGenerationFinding.NAME
  override val description: String =
    "Finds modules which have android resources R file generation enabled, " +
      "but don't actually use any resources from the module"

  override suspend fun check(project: McProject): List<UnusedResourcesGenerationFinding> {

    val resourcesEnabled = (project.platformPlugin as? AndroidLibraryPlugin)
      ?.androidResourcesEnabled == true

    if (!resourcesEnabled) return emptyList()

    fun findingList() = listOf(
      UnusedResourcesGenerationFinding(
        dependentProject = project,
        dependentPath = project.projectPath,
        buildFile = project.buildFile
      )
    )

    val usedLocally = project.sourceSets
      .keys
      .any { sourceSetName ->

        val rName = project.androidRDeclaredNameForSourceSetName(sourceSetName)
          ?: return@any false
        val references = project.referencesForSourceSetName(sourceSetName)

        references.contains(rName) ||
          references
            .containsAny(project.androidResourceDeclaredNamesForSourceSetName(sourceSetName)) ||
          references
            .containsAny(project.androidDataBindingDeclarationsForSourceSetName(sourceSetName))
      }

    if (usedLocally) return emptyList()

    val usedInDownstreamProject = project.dependents()
      .any { downstream ->
        downstream.project(project)
          .sourceSets
          .keys
          .any any2@{ sourceSetName ->

            val rName = project.androidRDeclaredNameForSourceSetName(sourceSetName)
              ?: return@any2 false

            val refsForSourceSet = project.projectCache
              .getValue(downstream.dependentProjectPath)
              .androidResourceReferencesForSourceSetName(sourceSetName)

            val resourceDeclarations = project
              .androidResourceDeclaredNamesForSourceSetName(sourceSetName)

            refsForSourceSet.contains(rName) ||
              refsForSourceSet.containsAny(resourceDeclarations)
          }
      }

    if (usedInDownstreamProject) return emptyList()

    return findingList()
  }

  override fun shouldApply(settings: ModuleCheckSettings): Boolean {
    return settings.checks.disableAndroidResources
  }
}
