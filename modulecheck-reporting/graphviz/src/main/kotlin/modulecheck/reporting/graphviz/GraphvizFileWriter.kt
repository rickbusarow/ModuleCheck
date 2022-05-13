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

import dispatch.core.launchIO
import kotlinx.coroutines.coroutineScope
import modulecheck.api.context.ProjectDepth
import modulecheck.config.ModuleCheckSettings
import modulecheck.utils.child
import java.io.File
import javax.inject.Inject

class GraphvizFileWriter @Inject constructor(
  private val settings: ModuleCheckSettings,
  private val graphvizFactory: GraphvizFactory
) {

  suspend fun write(depths: List<ProjectDepth>) {
    coroutineScope {

      val rootOrNull = settings.reports.graphs.outputDir?.let { File(it) }

      depths
        // Generate the low-depth graphs first, because their data is memoized and used to create the
        // graphs for high-depth projects.
        .sorted()
        .forEach { depth ->

          launchIO {

            val graphFile = rootOrNull?.child(
              depth.dependentPath.value.replace(":", File.separator),
              "${depth.sourceSetName.value}.dot"
            )
              ?: depth.dependentProject.projectDir.child(
                "build",
                "reports",
                "modulecheck",
                "graphs",
                "${depth.sourceSetName.value}.dot"
              )

            val depthReport = graphvizFactory.create(depth)

            graphFile.parentFile.mkdirs()
            graphFile.writeText(depthReport)
          }
        }
    }
  }
}
