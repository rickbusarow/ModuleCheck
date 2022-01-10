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

package modulecheck.api.context

import modulecheck.api.DepthFinding
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.SafeCache

data class Depths(
  private val delegate: SafeCache<SourceSetName, DepthFinding>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<Depths>
    get() = Key

  internal suspend fun populateAll() {
    project.sourceSets
      .keys
      .forEach { fetchForSourceSet(it) }
  }

  suspend fun get(key: SourceSetName): DepthFinding {
    return delegate.getOrPut(key) { fetchForSourceSet(key) }
  }

  private suspend fun fetchForSourceSet(sourceSetName: SourceSetName): DepthFinding {
    val (childDepth, children) = project.projectDependencies[sourceSetName]
      .map { it.project }
      .distinct()
      .map { it.depthForSourceSetName(SourceSetName.MAIN) }
      .groupBy { it.depth }
      .let {
        val max = it.keys.maxOrNull() ?: -1
        max to it[max].orEmpty()
      }

    return DepthFinding(
      dependentProject = project,
      dependentPath = project.path,
      depth = childDepth + 1,
      children = children,
      sourceSetName = sourceSetName,
      buildFile = project.buildFile
    )
  }

  companion object Key : ProjectContext.Key<Depths> {
    override suspend operator fun invoke(project: McProject): Depths {

      return Depths(SafeCache(), project)
    }
  }
}

suspend fun McProject.depths(): Depths = get(Depths).also { it.populateAll() }

suspend fun McProject.depthForSourceSetName(sourceSetName: SourceSetName): DepthFinding {
  return get(Depths).get(sourceSetName)
}
