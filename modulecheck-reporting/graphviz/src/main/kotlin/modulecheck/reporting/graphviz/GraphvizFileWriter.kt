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

import guru.nidi.graphviz.engine.Format
import modulecheck.api.context.ProjectDepth
import modulecheck.config.ModuleCheckSettings
import modulecheck.utils.child
import modulecheck.utils.createSafely
import modulecheck.utils.indentByBrackets
import java.io.File
import javax.inject.Inject

class GraphvizFileWriter @Inject constructor(
  private val settings: ModuleCheckSettings,
  private val graphvizFactory: GraphvizFactory
) {

  suspend fun write(depths: List<ProjectDepth>) {

    val rootOrNull = settings.reports.graphs.outputDir?.let { File(it) }

    depths
      // Generate the low-depth graphs first, because their data is memoized and used to create the
      // graphs for high-depth projects.
      .sorted()
      .forEach { depth ->

        val graphDir = rootOrNull?.child(
          depth.dependentPath.value.replace(":", File.separator)
        )
          ?: depth.dependentProject.projectDir.child(
            "build",
            "reports",
            "modulecheck",
            "graphs"
          )

        graphDir.mkdirs()

        val fileName = depth.sourceSetName.value

        val dotFile = graphDir.child("$fileName.dot")

        val graphviz = graphvizFactory.create(depth)

        val dotString = graphviz
          .render(Format.DOT)
          .toString()
          .indentByBrackets()

        dotFile.createSafely(dotString)

        // TODO - maybe add SVG and PNG generation.  Large graphs can take a long time to generate
        //  (~20 seconds for SVG with 500 modules), and PNGs for large graphs have huge file sizes.
        //  It might be a good idea to explicitly name the modules in the extension config, and/or
        //  to reserve the image generation only for the `-Graphs` gradle task.
        /*
        GraphvizCmdLineEngine()
          .timeout(1, MINUTES)
          .execute(
            dotString, Options.create().format(Format.SVG), Rasterizer.DEFAULT
          )
          .let {
            graphDir.child("$fileName.svg").createSafely(it.asString())
          }
         */
      }
  }
}
