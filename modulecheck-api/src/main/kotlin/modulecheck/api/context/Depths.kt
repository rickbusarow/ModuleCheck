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

package modulecheck.api.context

import modulecheck.api.DepthFinding
import modulecheck.finding.FindingName
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.model.sourceset.SourceSetName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.project
import modulecheck.utils.cache.SafeCache

data class Depths(
  private val delegate: SafeCache<SourceSetName, ProjectDepth>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<Depths>
    get() = Key

  /**
   * @return a [ProjectDepth] for each [SourceSet][modulecheck.model.dependency.McSourceSet] in
   *   this project.
   * @since 0.12.0
   */
  suspend fun all(): List<ProjectDepth> = project.sourceSets.map { get(it.key) }

  suspend fun get(key: SourceSetName): ProjectDepth {
    return delegate.getOrPut(key) { fetchForSourceSet(key) }
  }

  private suspend fun fetchForSourceSet(sourceSetName: SourceSetName): ProjectDepth {
    val (childDepth, children) = project.projectDependencies[sourceSetName]
      .map { it.project(project) }
      .distinct()
      .map { it.depthForSourceSetName(SourceSetName.MAIN) }
      .groupBy { it.depth }
      .let {
        val max = it.keys.maxOrNull() ?: -1
        max to it[max].orEmpty()
      }

    return ProjectDepth(
      dependentProject = project,
      dependentPath = project.projectPath,
      depth = childDepth + 1,
      children = children,
      sourceSetName = sourceSetName
    )
  }

  companion object Key : ProjectContext.Key<Depths> {
    override suspend operator fun invoke(project: McProject): Depths {

      return Depths(SafeCache(listOf(project.projectPath, Depths::class)), project)
    }
  }
}

suspend fun McProject.depths(): Depths = get(Depths)

suspend fun McProject.depthForSourceSetName(sourceSetName: SourceSetName): ProjectDepth {
  return get(Depths).get(sourceSetName)
}

data class ProjectDepth(
  val dependentProject: McProject,
  val dependentPath: StringProjectPath,
  val depth: Int,
  val children: List<ProjectDepth>,
  val sourceSetName: SourceSetName
) : Comparable<ProjectDepth> {
  private val treeCache = SafeCache<SourceSetName, Set<ProjectDepth>>(
    listOf(dependentProject.projectPath, ProjectDepth::class)
  )

  fun toFinding(name: FindingName): DepthFinding = DepthFinding(
    dependentProject = dependentProject,
    dependentPath = dependentPath,
    depth = depth,
    children = children.map { it.toFinding(name) },
    sourceSetName = sourceSetName,
    buildFile = dependentProject.buildFile
  )

  suspend fun fullTree(sourceSetName: SourceSetName = this.sourceSetName): Set<ProjectDepth> {
    return treeCache.getOrPut(sourceSetName) {
      val children = dependentProject
        .projectDependencies[sourceSetName]
        .flatMap {
          it.project(dependentProject.projectCache)
            .depthForSourceSetName(SourceSetName.MAIN)
            .fullTree(SourceSetName.MAIN)
        }
      children.toSet() + this
    }
  }

  override fun compareTo(other: ProjectDepth): Int {
    return depth.compareTo(other.depth)
  }
}
