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

package com.rickbusarow.modulecheck.kapt

import com.rickbusarow.modulecheck.Config
import com.rickbusarow.modulecheck.mcp
import com.rickbusarow.modulecheck.rule.AbstractRule
import org.gradle.api.Project

internal const val KAPT_PLUGIN_ID = "org.jetbrains.kotlin.kapt"
internal const val KAPT_PLUGIN_FUN = "kotlin(\"kapt\")"

class UnusedKaptRule(
  project: Project,
  alwaysIgnore: Set<String>,
  ignoreAll: Set<String>,
  private val kaptMatchers: List<KaptMatcher>
) : AbstractRule<UnusedKaptFinding>(
  project, alwaysIgnore, ignoreAll
) {

  override fun check(): List<UnusedKaptFinding> {
    val matchers = kaptMatchers.asMap()

    if (project.path in ignoreAll) return emptyList()

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
          .map { UnusedKaptProcessorFinding(project, it.coordinates, config) }

        val unusedPlugin =
          kaptDependencies.all().size == unused.size && plugins.hasPlugin(KAPT_PLUGIN_ID)

        if (unusedPlugin) {
          unused + UnusedKaptPluginFinding(project)
        } else {
          unused
        }
      }
    }
  }
}
