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

import kotlinx.coroutines.flow.toSet
import modulecheck.api.context.referencesForSourceSetName
import modulecheck.api.rule.ModuleCheckRule
import modulecheck.api.settings.ChecksSettings
import modulecheck.core.UnusedPluginFinding
import modulecheck.project.AndroidMcProject
import modulecheck.project.McProject

const val KOTLIN_ANDROID_EXTENSIONS_PLUGIN_ID = "org.jetbrains.kotlin.android.extensions"
private const val KOTLIN_ANDROID_EXTENSIONS_PLUGIN_FUN = "kotlin(\"android-extensions\")"

class UnusedKotlinAndroidExtensionsRule : ModuleCheckRule<UnusedPluginFinding> {

  override val id = "UnusedKotlinAndroidExtensions"
  override val description = "Finds modules which have Kotlin AndroidExtensions enabled, " +
    "but don't actually use any synthetic imports"

  override suspend fun check(project: McProject): List<UnusedPluginFinding> {
    val androidProject = project as? AndroidMcProject ?: return emptyList()

    // no chance of a finding if the feature's already disabled
    if (!androidProject.kotlinAndroidExtensionEnabled) return emptyList()

    val usedInProject = project.sourceSets.keys
      .any { sourceSetName ->
        val syntheticReference = "kotlinx.android.synthetic"
        project.referencesForSourceSetName(sourceSetName)
          .toSet().any {
            it.startsWith(syntheticReference)
          }
      }

    return if (usedInProject) emptyList()
    else listOf(
      UnusedPluginFinding(
        dependentProject = project, dependentPath = project.path, buildFile = project.buildFile,
        findingName = "unusedKotlinAndroidExtensions",
        pluginId = KOTLIN_ANDROID_EXTENSIONS_PLUGIN_ID,
        kotlinPluginFunction = KOTLIN_ANDROID_EXTENSIONS_PLUGIN_FUN
      )
    )
  }

  override fun shouldApply(checksSettings: ChecksSettings): Boolean {
    return checksSettings.unusedKotlinAndroidExtensions
  }
}
