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

import modulecheck.api.context.kaptDependencies
import modulecheck.config.ChecksSettings
import modulecheck.config.KaptMatcher
import modulecheck.config.ModuleCheckSettings
import modulecheck.config.asMap
import modulecheck.core.UnusedPluginFinding
import modulecheck.core.context.unusedKaptProcessors
import modulecheck.core.kapt.defaultKaptMatchers
import modulecheck.finding.Finding
import modulecheck.finding.FindingName
import modulecheck.project.McProject
import modulecheck.project.PluginDefinition

class UnusedKaptPluginRule(
  private val settings: ModuleCheckSettings
) : DocumentedRule<Finding>() {

  private val kaptMatchers: List<KaptMatcher>
    get() = settings.additionalKaptMatchers + defaultKaptMatchers

  override val name = FindingName("unused-kapt-plugin")
  override val description = "Warns if the kapt plugin is applied, but unused"

  override suspend fun check(project: McProject): List<Finding> {
    if (!project.hasKapt) return emptyList()

    val matchers = kaptMatchers.asMap()

    val kaptDependencies = project.kaptDependencies()

    val processorIsUsed = project
      .configurations
      .keys
      .filter { it.value.startsWith("kapt") }
      .any { configName ->

        val processors = kaptDependencies.get(configName)
          .filter { matchers.containsKey(it.name) }

        if (processors.isEmpty()) return@any false

        val unusedAndNotSuppressed = project.unusedKaptProcessors()
          .get(configName, settings)
          .filterNot { it.isSuppressed.await() }

        unusedAndNotSuppressed.size != processors.size
      }

    return if (processorIsUsed) {
      emptyList()
    } else {
      listOf(
        UnusedPluginFinding(
          dependentProject = project,
          dependentPath = project.path,
          buildFile = project.buildFile,
          findingName = name,
          pluginDefinition = PluginDefinition(
            name = "kapt",
            qualifiedId = KAPT_PLUGIN_ID,
            legacyIdOrNull = KAPT_ALTERNATE_PLUGIN_ID,
            precompiledAccessorOrNull = null,
            kotlinFunctionArgumentOrNull = KAPT_PLUGIN_FUN
          )
        )
      )
    }
  }

  override fun shouldApply(checksSettings: ChecksSettings): Boolean {
    return checksSettings.unusedKapt
  }
}
