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

package modulecheck.rule.impl

import modulecheck.api.context.kaptDependencies
import modulecheck.config.CodeGeneratorBinding
import modulecheck.config.ModuleCheckSettings
import modulecheck.config.asMap
import modulecheck.config.internal.defaultCodeGeneratorBindings
import modulecheck.core.context.unusedKaptProcessors
import modulecheck.finding.Finding
import modulecheck.finding.FindingName
import modulecheck.finding.UnusedPluginFinding
import modulecheck.parsing.gradle.model.PluginDefinition
import modulecheck.project.McProject
import modulecheck.utils.mapToSet
import javax.inject.Inject

class UnusedKaptPluginRule @Inject constructor(
  private val settings: ModuleCheckSettings
) : DocumentedRule<Finding>() {

  private val generatorBindings: List<CodeGeneratorBinding>
    get() = settings.additionalCodeGenerators
      .plus(
        @Suppress("DEPRECATION")
        settings.additionalKaptMatchers
          .mapToSet { it.toCodeGeneratorBinding() }
      )
      .plus(defaultCodeGeneratorBindings())

  override val name = FindingName("unused-kapt-plugin")
  override val description = "Warns if the kapt plugin is applied, but unused"

  override suspend fun check(project: McProject): List<Finding> {
    if (!project.hasKapt) return emptyList()

    val matchers = generatorBindings.asMap()

    val kaptDependencies = project.kaptDependencies()

    val processorIsUsed = project
      .configurations
      .keys
      .filter { it.isKapt() }
      .any { configName ->

        val processors = kaptDependencies.get(configName)
          .filter { matchers.containsKey(it.name) }

        if (processors.isEmpty()) return@any false

        val unusedAndNotSuppressed = project.unusedKaptProcessors()
          .get(configName, settings)
          .filterNot { it.isSuppressed.await() }

        processors.size - unusedAndNotSuppressed.size != 0
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

  override fun shouldApply(settings: ModuleCheckSettings): Boolean {
    return settings.checks.unusedKapt
  }
}
