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

import modulecheck.api.KaptMatcher
import modulecheck.api.asMap
import modulecheck.api.context.kaptDependencies
import modulecheck.api.context.referencesForSourceSetName
import modulecheck.api.finding.Finding
import modulecheck.api.rule.RuleName
import modulecheck.api.settings.ChecksSettings
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.core.kapt.UnusedKaptProcessorFinding
import modulecheck.core.kapt.defaultKaptMatchers
import modulecheck.parsing.source.Reference
import modulecheck.project.McProject
import modulecheck.utils.LazySet
import modulecheck.utils.any

const val KAPT_PLUGIN_ID = "org.jetbrains.kotlin.kapt"
const val KAPT_ALTERNATE_PLUGIN_ID = "kotlin-kapt"
internal const val KAPT_PLUGIN_FUN = "kotlin(\"kapt\")"

class UnusedKaptProcessorRule(
  private val settings: ModuleCheckSettings
) : DocumentedRule<Finding>() {

  private val kaptMatchers: List<KaptMatcher>
    get() = settings.additionalKaptMatchers + defaultKaptMatchers

  override val name = RuleName("unused-kapt-processor")
  override val description = "Finds unused kapt processor dependencies " +
    "and warns if the kapt plugin is applied but unused"

  override suspend fun check(project: McProject): List<Finding> {
    val matchers = kaptMatchers.asMap()

    val kaptDependencies = project.kaptDependencies()

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
              ruleName = name,
              dependentProject = project,
              dependentPath = project.path,
              buildFile = project.buildFile,
              oldDependency = processor,
              configurationName = configName
            )
          }

        unusedProcessorFindings
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
