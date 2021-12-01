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

import dispatch.core.launchIO
import kotlinx.coroutines.coroutineScope
import modulecheck.api.DepthFinding
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.utils.child
import java.io.File
import javax.inject.Inject

class GraphvizFileWriter @Inject constructor(
  private val settings: ModuleCheckSettings,
  private val graphvizFactory: GraphvizFactory
) {

  suspend fun write(depths: List<DepthFinding>) = coroutineScope {

    val rootOrNull = settings.reports.graphs.outputPath?.let { File(it) }

    depths
      .filter {
        // Don't generate a graph if the SourceSet doesn't exist at all.
        // For example, if it's an Android project there will be an `androidTest` SourceSet,
        // but if there are no `androidTestImplementation` dependencies and no files, then skip it.
        it.depth != 0 ||
          it.dependentProject.sourceSets[it.sourceSetName]?.hasExistingSourceFiles() == true
      }
      // Generate the low-depth graphs first, because their data is memoized and used to create the
      // graphs for high-depth projects.
      .sorted()
      .forEach { depth ->

        launchIO {

          val root = rootOrNull ?: depth.dependentProject.projectDir

          val graphFile = root.child(
            "build",
            "reports",
            "modulecheck",
            "graph",
            "${depth.sourceSetName.value}.dot"
          )

          val depthReport = graphvizFactory.create(depth)

          graphFile.parentFile.mkdirs()
          graphFile.writeText(depthReport)
        }
      }
  }
}
