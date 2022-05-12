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

import modulecheck.api.context.referencesForSourceSetName
import modulecheck.config.ChecksSettings
import modulecheck.core.UnusedPluginFinding
import modulecheck.finding.FindingName
import modulecheck.parsing.gradle.model.PluginDefinition
import modulecheck.parsing.source.asExplicitKotlinReference
import modulecheck.project.McProject
import modulecheck.utils.any

const val KOTLIN_ANDROID_EXTENSIONS_PLUGIN_ID = "org.jetbrains.kotlin.android.extensions"
private const val KOTLIN_ANDROID_EXTENSIONS_PLUGIN_FUN = "android-extensions"

class UnusedKotlinAndroidExtensionsRule : DocumentedRule<UnusedPluginFinding>() {

  override val name = FindingName("unused-kotlin-android-extensions")
  override val description = "Finds modules which have Kotlin AndroidExtensions enabled, " +
    "but don't actually use any synthetic imports"

  private val parcelizeImport = "kotlinx.android.parcel.Parcelize".asExplicitKotlinReference()
  private val syntheticReferencePackage = "kotlinx.android.synthetic".asExplicitKotlinReference()

  override suspend fun check(project: McProject): List<UnusedPluginFinding> {
    val androidPlugin = project.platformPlugin.asAndroidOrNull() ?: return emptyList()

    // no chance of a finding if the feature's already disabled
    if (!androidPlugin.kotlinAndroidExtensionEnabled) return emptyList()

    val usedInProject = androidPlugin.sourceSets.keys
      .any { sourceSetName ->
        project.referencesForSourceSetName(sourceSetName)
          .any { it == parcelizeImport || it.startsWith(syntheticReferencePackage) }
      }

    return if (usedInProject) emptyList()
    else listOf(
      UnusedPluginFinding(
        dependentProject = project,
        dependentPath = project.path,
        buildFile = project.buildFile,
        findingName = name,
        pluginDefinition = PluginDefinition(
          name = "Kotlin Android Extensions",
          qualifiedId = KOTLIN_ANDROID_EXTENSIONS_PLUGIN_ID,
          legacyIdOrNull = null,
          precompiledAccessorOrNull = null,
          kotlinFunctionArgumentOrNull = KOTLIN_ANDROID_EXTENSIONS_PLUGIN_FUN
        )
      )
    )
  }

  override fun shouldApply(checksSettings: ChecksSettings): Boolean {
    return checksSettings.unusedKotlinAndroidExtensions
  }
}
