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

package modulecheck.api.context

import modulecheck.api.DepthFinding
import modulecheck.parsing.McProject
import modulecheck.parsing.ProjectContext
import modulecheck.parsing.SourceSetName
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.collections.MutableMap.MutableEntry

data class Depths(
  internal val delegate: ConcurrentMap<SourceSetName, DepthFinding>,
  private val project: McProject
) : ConcurrentMap<SourceSetName, DepthFinding> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<Depths>
    get() = Key

  override val entries: MutableSet<MutableEntry<SourceSetName, DepthFinding>>
    get() {
      populateAll()
      return delegate.entries
    }

  override val values: MutableCollection<DepthFinding>
    get() {
      populateAll()
      return delegate.values
    }
  override val keys: MutableSet<SourceSetName>
    get() {
      populateAll()
      return delegate.keys
    }

  internal fun populateAll() {
    project.sourceSets
      .keys
      .forEach { fetchForSourceSet(it) }
  }

  override operator fun get(key: SourceSetName): DepthFinding {
    return delegate.getOrPut(key) { fetchForSourceSet(key) }
  }

  private fun fetchForSourceSet(key: SourceSetName): DepthFinding {
    val (childDepth, children) = project.projectDependencies[key]
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
      sourceSetName = key,
      buildFile = project.buildFile
    )
  }

  companion object Key : ProjectContext.Key<Depths> {
    override operator fun invoke(project: McProject): Depths {

      return Depths(ConcurrentHashMap(), project)
    }
  }
}

val McProject.depths: Depths get() = get(Depths).also { it.populateAll() }

fun McProject.depthForSourceSetName(sourceSetName: SourceSetName): DepthFinding {
  return get(Depths)[sourceSetName]
}
