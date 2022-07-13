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

package modulecheck.runtime.test

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
import modulecheck.utils.remove
import modulecheck.utils.requireNotNull
import kotlin.properties.Delegates

private typealias ProjectPath = String

@Suppress("LocalVariableName", "VariableNaming")
internal fun String.parseReportOutput(): List<Pair<ProjectPath, List<ProjectFindingReport>>> {

  val DELIM = "\u200B"
  val FIXED = "✔"
  val ERROR = "X"

  val resultLineStarters = listOf(FIXED, ERROR)

  // use a LinkedHashMap so that the order of paths is preserved
  val map = LinkedHashMap<ProjectPath, MutableList<ProjectFindingReport>>()

  val lines = lines()
    .filterNot { it.isBlank() || it.isEmpty() }
    .filterNot {
      it.trim()
        .replace(" {2,}".toRegex(), " ") == "configuration dependency name source build file"
    }
    .map { line ->

      line
        .removePrefix(DELIM)
        .trim()
        .removePrefix(DELIM)
        .trim()
        .remove("/[^/]*/build\\.gradle(?:\\.kts)?".toRegex())
    }

  var currentPath by Delegates.notNull<String>()

  lines.forEach { line ->
    when {
      line.startsWith(":") -> {
        require(map[line] == null) {
          "An entry for `$line` already exists in the map of results.  " +
            "Are there two header lines for this path in the console output?"
        }
        map[line] = mutableListOf()
        currentPath = line
        return@forEach
      }

      resultLineStarters.any { line.startsWith(it) } -> {

        val split = line.split(DELIM)
          .map { segment -> segment.trim().takeIf { it.isNotBlank() } }

        val (fixedString, configuration, dependency, nameWithTokens, source) = split
        // Destructuring a List<String> only supports 5 components, so do the 6th manually.
        val positionRaw = split[5]

        val name = nameWithTokens.requireNotNull().trim()

        val position = positionRaw?.let {
          """.*\((\d+, \d+)\):""".toRegex().find(it)?.destructured?.component1()
        }

        val fixed = when (fixedString) {
          FIXED -> true
          ERROR -> false
          else -> error(
            "The parsed string `$fixedString` must be one of " +
              "[${resultLineStarters.joinToString { "`$it`" }}]."
          )
        }

        val t = when (name) {
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

          "disable-android-resources" -> disableAndroidResources(fixed = fixed, position = position)
          else -> error("could not parse a finding result type for name of `$name`.")
        }

        map.getValue(currentPath).add(t)
      }
    }
  }

  return map.toList().map { it.first to it.second.sortedBy { it::class.java.simpleName } }
}
