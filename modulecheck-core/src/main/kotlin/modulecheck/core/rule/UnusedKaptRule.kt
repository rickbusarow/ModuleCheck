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
import modulecheck.api.Project2
import modulecheck.api.asMap
import modulecheck.api.context.KaptDependencies
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.core.kapt.UnusedKaptFinding
import modulecheck.core.kapt.UnusedKaptPluginFinding
import modulecheck.core.kapt.UnusedKaptProcessorFinding
import modulecheck.core.kapt.defaultKaptMatchers

const val KAPT_PLUGIN_ID = "org.jetbrains.kotlin.kapt"
internal const val KAPT_PLUGIN_FUN = "kotlin(\"kapt\")"

class UnusedKaptRule(
  override val settings: ModuleCheckSettings
) : ModuleCheckRule<UnusedKaptFinding>() {

  private val kaptMatchers: List<KaptMatcher>
    get() = settings.additionalKaptMatchers + defaultKaptMatchers

  override val id = "UnusedKapt"
  override val description = "Finds unused kapt processor dependencies " +
    "and warns if the kapt plugin is applied but unused"

  override fun check(project: Project2): List<UnusedKaptFinding> {
    val matchers = kaptMatchers.asMap()

    return project
      .configurations
      .keys
      .map { configName ->
        configName to project.importsForSourceSetName(configName) +
          project.extraPossibleReferencesForSourceSetName(configName)
      }
      .flatMap { (configurationName, imports) ->

        val processors = project.kaptDependenciesForConfig(configurationName)

        val unused = processors.filterNot { processor ->

          matchers[processor.coordinates]?.let { matcher ->

            matcher.annotationImports.none { annotationRegex ->

              imports.any { import ->

                annotationRegex.matches(import)
              }
            }
          } == true
        }
          .map {
            UnusedKaptProcessorFinding(
              project.path,
              project.buildFile,
              it.coordinates,
              configurationName
            )
          }

        val unusedPlugin = project
          .context[KaptDependencies]
          .values
          .flatten()
          .size == unused.size && project.hasKapt && unused.isNotEmpty()

        if (unusedPlugin) {
          unused + UnusedKaptPluginFinding(project.path, project.buildFile)
        } else {
          unused
        }
      }
  }
}
