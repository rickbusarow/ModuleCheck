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
import modulecheck.config.ModuleCheckSettings
import modulecheck.core.context.overshotDependencies
import modulecheck.core.context.unusedKaptProcessors
import modulecheck.finding.Finding
import modulecheck.finding.FindingName
import modulecheck.finding.UnusedPluginFinding
import modulecheck.model.dependency.MightHaveCodeGeneratorBinding
import modulecheck.model.dependency.PluginDefinition
import modulecheck.project.McProject
import modulecheck.utils.flatMapToSet
import modulecheck.utils.lazy.lazyDeferred
import javax.inject.Inject

class UnusedKaptPluginRule @Inject constructor() : DocumentedRule<Finding>() {

  override val name = FindingName("unused-kapt-plugin")
  override val description = "Warns if the kapt plugin is applied, but unused"

  override suspend fun check(project: McProject): List<Finding> {
    if (!project.hasKapt) return emptyList()

    val kaptDependencies = project.kaptDependencies()

    val overshotKapt = project.configurations.keys
      .filter { it.isKapt() }
      .flatMapToSet { project.overshotDependencies().get(it) }

    val processorIsUsed = lazyDeferred {
      project
        .configurations
        .keys
        .filter { it.isKapt() }
        .any { configName ->

          val processors = kaptDependencies.get(configName)
            .filterIsInstance<MightHaveCodeGeneratorBinding>()
            .filter { it.codeGeneratorBindingOrNull != null }

          if (processors.isEmpty()) return@any false

          val unusedAndNotSuppressed = project.unusedKaptProcessors()
            .get(configName)
            .filterNot { it.isSuppressed.await() }

          processors.size - unusedAndNotSuppressed.size != 0
        }
    }

    return when {
      overshotKapt.isNotEmpty() -> emptyList()
      processorIsUsed.await() -> emptyList()
      else -> listOf(
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
