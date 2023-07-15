/*
 * Copyright (C) 2021-2023 Rick Busarow
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

package modulecheck.runtime.test

import modulecheck.reporting.console.ReportFactory
import modulecheck.runtime.test.ProjectFindingReport.depth
import modulecheck.runtime.test.ProjectFindingReport.disableAndroidResources
import modulecheck.runtime.test.ProjectFindingReport.disableViewBinding
import modulecheck.runtime.test.ProjectFindingReport.inheritedDependency
import modulecheck.runtime.test.ProjectFindingReport.mustBeApi
import modulecheck.runtime.test.ProjectFindingReport.overshot
import modulecheck.runtime.test.ProjectFindingReport.redundant
import modulecheck.runtime.test.ProjectFindingReport.unsortedDependencies
import modulecheck.runtime.test.ProjectFindingReport.unsortedPlugins
import modulecheck.runtime.test.ProjectFindingReport.unusedDependency
import modulecheck.runtime.test.ProjectFindingReport.unusedKaptPlugin
import modulecheck.runtime.test.ProjectFindingReport.unusedKaptProcessor
import modulecheck.runtime.test.ProjectFindingReport.unusedKotlinAndroidExtensions
import modulecheck.runtime.test.ProjectFindingReport.useAnvilFactories
import modulecheck.utils.noAnsi
import modulecheck.utils.remove
import kotlin.properties.Delegates

private typealias ProjectPath = String

@Suppress("VariableNaming")
internal fun String.parseReportOutput(): List<Pair<ProjectPath, List<ProjectFindingReport>>> {

  val resultLineStarters = listOf(ReportFactory.FIXED, ReportFactory.ERROR)

  // use a LinkedHashMap so that the order of paths is preserved
  val map = LinkedHashMap<ProjectPath, MutableList<ProjectFindingReport>>()

  var currentPath by Delegates.notNull<String>()
  var fixedIndex = -1
  var configurationIndex = -1
  var dependencyIndex = -1
  var ruleNameIndex = -1
  var sourceIndex = -1
  var positionIndex = -1

  val currentPathRegex = " *:\\S.*".toRegex()
  val header = " *configuration *dependency *name *source *build file *".toRegex()
  val filePathRegex = """/[^/]*/build\.gradle(?:\.kts)?:""".toRegex()

  lineSequence()
    .map { it.noAnsi() }
    .filter { it.isNotBlank() }
    .map { it.remove(filePathRegex) }
    .onEach { println(" line 1 ---  $it") }
    .forEach { line ->
      val trimmed = line.trim()

      when {
        line.matches(header) -> {
          configurationIndex = line.indexOf("configuration")
          fixedIndex = configurationIndex - (ReportFactory.PADDING + 2)
          dependencyIndex = line.indexOf("dependency")
          ruleNameIndex = line.indexOf("name")
          sourceIndex = line.indexOf("source")
          positionIndex = line.indexOf("build file")
        }

        line.matches(currentPathRegex) -> {
          require(map[trimmed] == null) {
            "An entry for `$trimmed` already exists in the map of results.  " +
              "Are there two header lines for this path in the console output?"
          }
          map[trimmed] = mutableListOf()
          currentPath = trimmed
        }

        resultLineStarters.any { trimmed.startsWith(it) } -> {

          val fixedString = line.substring(fixedIndex until configurationIndex).trim()

          val configuration = line.substring(configurationIndex until dependencyIndex)
            .trim()
            .takeIf { it.isNotBlank() }
          val dependency = line.substring(dependencyIndex until ruleNameIndex)
            .trim()
            .takeIf { it.isNotBlank() }
          val ruleName = line.substring(ruleNameIndex until sourceIndex).trim()
          val source = line.substring(sourceIndex until positionIndex).trim().takeIf {
            it.isNotBlank()
          }
          val position = line.drop(positionIndex).trim()
            .takeIf { it.isNotBlank() }
            ?.remove("""[():]""".toRegex())

          val fixed = when (fixedString) {
            ReportFactory.FIXED -> true
            ReportFactory.ERROR -> false
            else -> error(
              "The parsed string `$fixedString` must be one of " +
                "[${resultLineStarters.joinToString { "`$it`" }}]."
            )
          }

          val t = when (ruleName) {
            "inherited-dependency" -> inheritedDependency(
              fixed,
              configuration,
              dependency,
              source,
              position
            )

            "must-be-api" -> mustBeApi(
              fixed = fixed,
              configuration = configuration,
              dependency = dependency,
              position = position
            )

            "overshot-dependency" -> overshot(
              fixed = fixed,
              configuration = configuration,
              dependency = dependency,
              position = position
            )

            "redundant-dependency" -> redundant(
              fixed = fixed,
              configuration = configuration,
              dependency = dependency,
              source = source,
              position = position
            )

            "unused-dependency" -> unusedDependency(
              fixed = fixed,
              configuration = configuration,
              dependency = dependency,
              position = position
            )

            "project-depth" -> depth(fixed)
            "use-anvil-factory-generation" -> useAnvilFactories(fixed)
            "disable-view-binding" -> disableViewBinding(fixed = fixed, position = position)
            "sort-dependencies" -> unsortedDependencies(fixed)
            "sort-plugins" -> unsortedPlugins(fixed)
            "unused-kapt-plugin" -> unusedKaptPlugin(
              fixed = fixed,
              dependency = dependency,
              position = position
            )

            "unused-kapt-processor" -> unusedKaptProcessor(
              fixed = fixed,
              configuration = configuration,
              dependency = dependency,
              position = position
            )

            "unused-kotlin-android-extensions" -> unusedKotlinAndroidExtensions(
              fixed = fixed,
              position = position
            )

            "disable-android-resources" -> disableAndroidResources(
              fixed = fixed,
              position = position
            )

            else -> error("could not parse a finding result type for name of `$ruleName`.")
          }

          map.getValue(currentPath).add(t)
        }
      }
    }

  return map.toList()
}
