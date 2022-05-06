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
import modulecheck.api.context.referencesForSourceSetName
import modulecheck.config.ChecksSettings
import modulecheck.config.KaptMatcher
import modulecheck.config.ModuleCheckSettings
import modulecheck.config.asMap
import modulecheck.core.UnusedPluginFinding
import modulecheck.core.kapt.UnusedKaptProcessorFinding
import modulecheck.core.kapt.defaultKaptMatchers
import modulecheck.finding.Finding
import modulecheck.finding.FindingName
import modulecheck.parsing.source.Reference
import modulecheck.project.McProject
import modulecheck.utils.LazySet
import modulecheck.utils.any

class UnusedKaptPluginRule(
  private val settings: ModuleCheckSettings
) : DocumentedRule<Finding>() {

  private val kaptMatchers: List<KaptMatcher>
    get() = settings.additionalKaptMatchers + defaultKaptMatchers

  override val name = FindingName("unused-kapt-plugin")
  override val description = "Warns if the kapt plugin is applied, but unused"

  override suspend fun check(project: McProject): List<Finding> {
    val matchers = kaptMatchers.asMap()

    val kaptDependencies = project.kaptDependencies()
    val allKaptDependencies = kaptDependencies.all()

    return project
      .configurations
      .keys
      .filter { it.value.startsWith("kapt") }
      .flatMap { configName ->

        val processors = kaptDependencies.get(configName)

        val references = project.referencesForSourceSetName(configName.toSourceSetName())

        // unused means that none of the processor's annotations are used in any import
        val unusedProcessors = processors
          .filterNot {
            val matcher = matchers[it.name] ?: return@filterNot true

            matcher.matchedIn(references)
          }

        val unusedProcessorFindings = unusedProcessors
          .map { processor ->
            UnusedKaptProcessorFinding(
              findingName = name,
              dependentProject = project,
              dependentPath = project.path,
              buildFile = project.buildFile,
              oldDependency = processor,
              configurationName = configName
            )
          }

        val pluginIsUnused = allKaptDependencies.size == unusedProcessorFindings.size &&
          project.hasKapt && unusedProcessorFindings.isNotEmpty()

        if (pluginIsUnused) {
          listOf(
            UnusedPluginFinding(
              dependentProject = project,
              dependentPath = project.path,
              buildFile = project.buildFile,
              findingName = name,
              pluginId = KAPT_PLUGIN_ID,
              alternatePluginId = KAPT_ALTERNATE_PLUGIN_ID,
              kotlinPluginFunction = KAPT_PLUGIN_FUN
            )
          )
        } else {
          emptyList()
        }
      }
  }

  override fun shouldApply(checksSettings: ChecksSettings): Boolean {
    return checksSettings.unusedKapt
  }

  private suspend fun KaptMatcher.matchedIn(
    references: LazySet<Reference>
  ): Boolean = annotationImports
    .map { it.toRegex() }
    .any { annotationRegex ->

      references.any { referenceName ->
        annotationRegex.matches(referenceName.name)
      }
    }
}
