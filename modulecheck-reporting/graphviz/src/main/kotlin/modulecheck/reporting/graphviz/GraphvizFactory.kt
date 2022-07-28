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

package modulecheck.reporting.graphviz

import guru.nidi.graphviz.attribute.Arrow
import guru.nidi.graphviz.attribute.Color
import guru.nidi.graphviz.attribute.Label
import guru.nidi.graphviz.attribute.Label.Location.TOP
import guru.nidi.graphviz.attribute.Rank
import guru.nidi.graphviz.attribute.Rank.RankDir
import guru.nidi.graphviz.attribute.Rank.RankType.SAME
import guru.nidi.graphviz.attribute.Shape
import guru.nidi.graphviz.attribute.Style
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.model.Factory
import guru.nidi.graphviz.model.Factory.mutGraph
import guru.nidi.graphviz.model.Factory.mutNode
import guru.nidi.graphviz.model.Factory.node
import modulecheck.api.context.ProjectDepth
import modulecheck.model.dependency.ProjectDependency
import modulecheck.parsing.gradle.model.ProjectPath
import modulecheck.parsing.gradle.model.TypeSafeProjectPathResolver
import modulecheck.project.isAndroid
import modulecheck.reporting.graphviz.GraphvizFactory.Colors.API_LINE
import modulecheck.reporting.graphviz.GraphvizFactory.Colors.BLACK
import modulecheck.reporting.graphviz.GraphvizFactory.Colors.IMPLEMENTATION_LINE
import modulecheck.reporting.graphviz.GraphvizFactory.Colors.JAVA_BLUE
import modulecheck.utils.applyEach
import javax.inject.Inject

/**
 * Creates a [Graphviz] model of a dependency graph from a given [ProjectDepth] root.
 *
 * @property typeSafeProjectPathResolver used to resolve project paths from type-safe project
 *   accessors
 * @since 0.12.0
 */
class GraphvizFactory @Inject constructor(
  private val typeSafeProjectPathResolver: TypeSafeProjectPathResolver
) {

  /**
   * Creates a [Graphviz] model of a dependency graph from a given [ProjectDepth] root.
   *
   * @param root the root of the dependency graph, starting at a single
   *   [SourceSet][modulecheck.parsing.gradle.model.SourceSet]
   * @return the graph model for this dependency graph
   * @since 0.12.0
   */
  suspend fun create(root: ProjectDepth): Graphviz {

    val allDepths = root.fullTree()
    val sourceSetName = root.sourceSetName

    val graphName = "${root.dependentPath.value} -- ${sourceSetName.value}"

    val rootGraph = mutGraph()
      .setStrict(true)
      .setDirected(true)
      .setCluster(true)
      .graphAttrs().apply {
        // ratio is the aspect ratio.  0.5625 is 16:9
        @Suppress("MagicNumber")
        add("ratio", 0.5625)
      }
      .add(
        Rank.dir(RankDir.TOP_TO_BOTTOM),
        Label.markdown("**$graphName**").locate(TOP)
      )
      .nodeAttrs().add(
        Style.combine(Style.ROUNDED, Style.FILLED),
        Shape.BOX
      )

    /* ranks */
    allDepths
      .groupBy { it.depth }
      .toSortedMap()
      .forEach { (_, sameRank) ->

        mutGraph()
          .graphAttrs()
          .add(Rank.inSubgraph(SAME))
          .applyEach(
            sameRank
              .distinctBy { it.dependentPath }
              .sortedBy { it.dependentPath }
          ) { projectDepth ->
            add(
              mutNode(projectDepth.pathString(), false)
                .add(projectDepth.nodeColor())
            )
          }
          .addTo(rootGraph)
      }

    /* edges */
    allDepths.sortedBy { it.dependentPath }
      .forEach { depthFinding ->
        depthFinding.dependentProject
          .projectDependencies[depthFinding.sourceSetName]
          .sortedBy { it.path }
          .forEach { cpd ->

            val lineColor = cpd.lineColor()

            rootGraph.add(
              node(depthFinding.pathString())
                .link(
                  Factory.to(node(cpd.path.pathString()))
                    .with(Arrow.NORMAL, Style.BOLD, lineColor)
                )
            )
          }
      }

    return Graphviz.fromGraph(rootGraph)
  }

  private fun ProjectDepth.pathString(): String {
    return dependentPath.pathValue(typeSafeProjectPathResolver)
  }

  private fun ProjectDepth.nodeColor(): Color {
    return when {
      dependentProject.isAndroid() -> Colors.ANDROID_GREEN.fill()
      else -> Colors.JAVA_ORANGE.fill()
    }
  }

  private fun ProjectPath.pathString(): String {
    return pathValue(typeSafeProjectPathResolver)
  }

  private fun ProjectDependency.lineColor(): Color {
    return when {
      configurationName.isApi() -> API_LINE
      configurationName.isKapt() -> JAVA_BLUE
      configurationName.isImplementation() -> IMPLEMENTATION_LINE
      else -> BLACK
    }
  }

  private object Colors {
    val ANDROID_GREEN: Color = Color.rgb("A4C639")
    val API_RED: Color = Color.rgb("AA0000")
    val BLACK: Color = Color.rgb("000000")
    val IMPLEMENTATION_GREEN: Color = Color.rgb("007744")
    val JAVA_BLUE: Color = Color.rgb("5382A1")
    val JAVA_ORANGE: Color = Color.rgb("F89820")
    val API_LINE: Color = Color.rgb("FF6347")
    val IMPLEMENTATION_LINE: Color = Color.rgb("FF6347")
  }
}
