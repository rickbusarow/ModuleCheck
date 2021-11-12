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

import modulecheck.api.KaptMatcher
import modulecheck.api.ModuleCheckRule
import modulecheck.api.asMap
import modulecheck.api.context.*
import modulecheck.api.settings.ChecksSettings
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.core.kapt.UnusedKaptFinding
import modulecheck.core.kapt.UnusedKaptPluginFinding
import modulecheck.core.kapt.UnusedKaptProcessorFinding
import modulecheck.core.kapt.defaultKaptMatchers
import modulecheck.parsing.McProject

const val KAPT_PLUGIN_ID = "org.jetbrains.kotlin.kapt"
internal const val KAPT_PLUGIN_FUN = "kotlin(\"kapt\")"

class UnusedKaptRule(
  private val settings: ModuleCheckSettings
) : ModuleCheckRule<UnusedKaptFinding> {

  private val kaptMatchers: List<KaptMatcher>
    get() = settings.additionalKaptMatchers + defaultKaptMatchers

  override val id = "UnusedKapt"
  override val description = "Finds unused kapt processor dependencies " +
    "and warns if the kapt plugin is applied but unused"

  override suspend fun check(project: McProject): List<UnusedKaptFinding> {
    val matchers = kaptMatchers.asMap()

    return project
      .configurations
      .keys
      .map { configName ->
        configName to project.importsForSourceSetName(configName.toSourceSetName()) +
          project.possibleReferencesForSourceSetName(configName.toSourceSetName())
      }
      .flatMap { (configurationName, imports) ->

        val processors = project.kaptDependencies()[configurationName].orEmpty()

        // unused means that none of the processor's annotations are used in any import
        val unusedMatchers = processors
          .mapNotNull { matchers[it.coordinates] }
          .filterNot { matcher ->

            matcher.matchedIn(imports)
          }

        val unusedFindings = unusedMatchers
          .map {
            UnusedKaptProcessorFinding(
              project.path,
              project.buildFile,
              it.processor,
              configurationName
            )
          }

        val unusedPlugin = project.get(KaptDependencies)
          .values
          .flatten()
          .size == unusedFindings.size && project.hasKapt && unusedFindings.isNotEmpty()

        if (unusedPlugin) {
          unusedFindings + UnusedKaptPluginFinding(project.path, project.buildFile)
        } else {
          unusedFindings
        }
      }
  }

  override fun shouldApply(checksSettings: ChecksSettings): Boolean {
    return checksSettings.unusedKapt
  }

  private fun KaptMatcher.matchedIn(
    imports: Set<ImportName>
  ): Boolean = annotationImports
    .any { annotationRegex ->

      imports.any { import ->

        annotationRegex.matches(import)
      }
    }
}
