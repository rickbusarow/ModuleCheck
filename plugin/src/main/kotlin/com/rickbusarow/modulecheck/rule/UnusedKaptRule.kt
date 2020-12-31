/*
 * Copyright (C) 2020 Rick Busarow
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

package com.rickbusarow.modulecheck.rule

import com.rickbusarow.modulecheck.Config
import com.rickbusarow.modulecheck.parser.KaptMatcher
import com.rickbusarow.modulecheck.parser.UnusedKapt
import com.rickbusarow.modulecheck.parser.asMap
import org.gradle.api.Project

class UnusedKaptRule(
  project: Project,
  alwaysIgnore: Set<String>,
  ignoreAll: Set<String>,
  private val kaptMatchers: List<KaptMatcher>
) : AbstractRule<UnusedKapt>(
  project, alwaysIgnore, ignoreAll
) {

  override fun check(): List<UnusedKapt> {
    val matchers = kaptMatchers.asMap()

    return project
      .moduleCheckProjects()
      .sorted()
      .filterNot { moduleCheckProject -> moduleCheckProject.path in ignoreAll }
      .flatMap { moduleCheckProject ->
        with(moduleCheckProject) mcp@{
          listOf(
            Config.KaptAndroidTest to androidTestImports,
            Config.Kapt to mainImports,
            Config.KaptTest to testImports
          ).flatMap { (config, imports) ->

            val processors = when (config) {
              Config.KaptAndroidTest -> kaptDependencies.androidTest
              Config.Kapt -> kaptDependencies.main
              Config.KaptTest -> kaptDependencies.test
              else -> throw IllegalArgumentException("")
            }

            processors.filter { coords ->

              matchers[coords.coordinates]?.let { matcher ->

                matcher.annotationImports.none { annotationRegex ->

                  imports.any { import ->

                    annotationRegex.matches(import)
                  }
                }
              } == true
            }
              .map { UnusedKapt(this.project, it.coordinates, config) }
          }
        }
      }
  }
}
