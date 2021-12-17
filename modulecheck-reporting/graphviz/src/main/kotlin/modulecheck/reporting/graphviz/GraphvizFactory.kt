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

package modulecheck.reporting.graphviz

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import modulecheck.api.DepthFinding
import modulecheck.api.context.depthForSourceSetName
import modulecheck.api.context.sourceSetDependencies
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.project.McProject
import modulecheck.project.isAndroid
import modulecheck.reporting.graphviz.GraphvizFactory.Color.ANDROID_GREEN
import modulecheck.reporting.graphviz.GraphvizFactory.Color.API_RED
import modulecheck.reporting.graphviz.GraphvizFactory.Color.BLACK
import modulecheck.reporting.graphviz.GraphvizFactory.Color.IMPLEMENTATION_GREEN
import modulecheck.reporting.graphviz.GraphvizFactory.Color.JAVA_ORANGE
import javax.inject.Inject

class GraphvizFactory @Inject constructor() {

  suspend fun create(root: DepthFinding): String = buildString {

    val allDepths = root.fullTree()

    appendLine("strict digraph DependencyGraph {")

    val inner = buildString {

      appendLine(
        """
        |ratio=0.5;
        |node [style="rounded,filled" shape=box];
        |
        """.trimMargin()
      )

      defineModuleBoxes(root)

      appendLine()

      defineEdges(allDepths)

      defineRanks(root)
    }

    append(inner.lines().joinToString("\n") { if (it.isBlank()) it else TAB + it })
    appendLine("}")
  }

  private suspend fun StringBuilder.defineModuleBoxes(root: DepthFinding) {
    root.dependentProject
      .allProjectDependencies(root.sourceSetName, includeSelf = true)
      .sortedBy { it.path }
      .distinctBy { it.path }
      .forEach {
        if (it.isAndroid()) {
          appendLine("${it.pathString()} [fillcolor = \"${ANDROID_GREEN.value}\"];")
        } else {
          appendLine("${it.pathString()} [fillcolor = \"${JAVA_ORANGE.value}\"];")
        }
      }
  }

  private suspend fun StringBuilder.defineEdges(allDepths: Set<DepthFinding>) {
    coroutineScope {
      allDepths.map { depthFinding ->
        async { depthFinding.edgesSection() }
      }
        .awaitAll()
        .sortedBy { it.first.path }
        .filter { it.second.isNotBlank() }
        .forEach { appendLine(it.second) }
    }
  }

  private fun DepthFinding.edgesSection(): Pair<McProject, String> {
    return dependentProject to buildString {
      dependentProject
        .projectDependencies[sourceSetName]
        .forEach { cpd ->

          val lineColor = when (cpd.configurationName) {
            ConfigurationName.compileOnlyApi -> IMPLEMENTATION_GREEN
            ConfigurationName.api -> API_RED
            ConfigurationName.kapt -> IMPLEMENTATION_GREEN
            ConfigurationName.implementation -> IMPLEMENTATION_GREEN
            ConfigurationName.compileOnly -> IMPLEMENTATION_GREEN
            ConfigurationName.compile -> IMPLEMENTATION_GREEN
            ConfigurationName.runtimeOnly -> IMPLEMENTATION_GREEN
            ConfigurationName.runtime -> IMPLEMENTATION_GREEN
            else -> BLACK
          }

          appendLine(
            "${dependentProject.pathString()} " +
              "-> " +
              "${cpd.project.pathString()} " +
              "[style = bold; color = \"${lineColor.value}\"];"
          )
        }
    }
  }

  private suspend fun StringBuilder.defineRanks(root: DepthFinding) {
    root.dependentProject.allProjectDependencies(root.sourceSetName, includeSelf = false)
      .map { it.depthForSourceSetName(SourceSetName.MAIN) }
      // If the root is a non-main source set, one of its dependencies may depend upon its main
      // sources.  For instance, if :lib-1 depends upon :lib-2 with `testImplementation`, but :lib-2
      // depends upon :lib-1 with `implementation`.
      .filterNot { it.dependentPath == root.dependentPath }
      .plus(root)
      .groupBy { it.depth }
      .toSortedMap()
      .entries
      .forEach { (_, allSame) ->

        val paths = allSame
          .sortedBy { it.dependentPath }
          .joinToString("; ", postfix = ";") { it.dependentProject.pathString() }

        appendLine("{rank = same; $paths}")
      }
  }

  private suspend fun McProject.allProjectDependencies(
    sourceSetName: SourceSetName,
    includeSelf: Boolean
  ): List<McProject> {
    return sourceSetDependencies()
      .get(sourceSetName)
      .distinct()
      .map { it.contributed.project }
      .let { deps ->
        if (includeSelf)
          deps + this
        else
          deps
      }
      .distinct()
  }

  private fun McProject.pathString(): String = "\"${this.path}\""

  enum class Color(val value: String) {
    ANDROID_GREEN("#A4C639"),
    JAVA_ORANGE("#F89820"),
    JAVA_BLUE("#5382A1"),
    API_RED("#AA0000"),
    IMPLEMENTATION_GREEN("#007744"),
    BLACK("#000000")
  }

  companion object {

    const val TAB = "  "

    const val API_LINE = "\"#FF6347\""
    const val IMPLEMENTATION_LINE = "\"#FF6347\""
  }
}
