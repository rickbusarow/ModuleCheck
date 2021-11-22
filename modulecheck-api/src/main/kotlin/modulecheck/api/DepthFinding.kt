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

package modulecheck.api

import modulecheck.api.context.depthForSourceSetName
import modulecheck.project.McProject
import modulecheck.project.SourceSetName
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class DepthFinding(
  val dependentProject: McProject,
  override val dependentPath: String,
  val depth: Int,
  val children: List<DepthFinding>,
  val sourceSetName: SourceSetName,
  override val buildFile: File
) : Finding, Comparable<DepthFinding> {

  override val message: String
    get() = "The longest path between this module and its leaf nodes"
  override val positionOrNull: Finding.Position?
    get() = null
  override val findingName: String
    get() = "depth"

  private val treeCache = ConcurrentHashMap<SourceSetName, Set<DepthFinding>>()

  suspend fun fullTree(sourceSetName: SourceSetName = this.sourceSetName): Set<DepthFinding> {

    val existing = treeCache[sourceSetName]

    if (existing != null) {
      return existing
    }

    val children = dependentProject
      .projectDependencies[sourceSetName]
      .flatMap {
        it.project.depthForSourceSetName(SourceSetName.MAIN)
          .fullTree(SourceSetName.MAIN)
      }

    val set = children.toSet() + this

    treeCache[sourceSetName] = set

    return set
  }

  override fun compareTo(other: DepthFinding): Int {
    return depth.compareTo(other.depth)
  }

  override fun toString(): String {
    return "DepthFinding(" +
      "dependentPath='$dependentPath', " +
      "depth=$depth, " +
      "children=$children, " +
      "sourceSetName=$sourceSetName" +
      ")"
  }
}
