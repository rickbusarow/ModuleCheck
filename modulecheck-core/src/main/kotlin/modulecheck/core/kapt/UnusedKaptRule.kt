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

package modulecheck.core.kapt

import modulecheck.api.Config
import modulecheck.api.Project2
import modulecheck.api.asMap
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.core.mcp
import modulecheck.core.rule.AbstractRule

const val KAPT_PLUGIN_ID = "org.jetbrains.kotlin.kapt"
internal const val KAPT_PLUGIN_FUN = "kotlin(\"kapt\")"

class UnusedKaptRule(
  override val settings: ModuleCheckSettings
) : AbstractRule<UnusedKaptFinding>() {

  private val kaptMatchers = settings.additionalKaptMatchers + defaultKaptMatchers

  override val id = "UnusedKapt"

  override fun check(project: Project2): List<UnusedKaptFinding> {
    val matchers = kaptMatchers.asMap()

    return with(project.mcp()) {
      listOf(
        Config.KaptAndroidTest to androidTestImports + androidTestExtraPossibleReferences,
        Config.Kapt to mainImports + mainExtraPossibleReferences,
        Config.KaptTest to testImports + testExtraPossibleReferences
      ).flatMap { (config, imports) ->

        val processors = when (config) {
          Config.KaptAndroidTest -> kaptDependencies.androidTest
          Config.Kapt -> kaptDependencies.main
          Config.KaptTest -> kaptDependencies.test
          else -> throw IllegalArgumentException("")
        }

        val unused = processors.filter { processor ->

          matchers[processor.coordinates]?.let { matcher ->

            matcher.annotationImports.none { annotationRegex ->

              imports.any { import ->

                annotationRegex.matches(import)
              }
            }
          } == true
        }
          .map { UnusedKaptProcessorFinding(project.buildFile, it.coordinates, config) }

        val unusedPlugin = project.kaptProcessors.all().size == unused.size && project.hasKapt

        if (unusedPlugin) {
          unused + UnusedKaptPluginFinding(project.buildFile)
        } else {
          unused
        }
      }
    }
  }
}
