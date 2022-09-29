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

import kotlinx.coroutines.flow.filterIsInstance
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.Generated
import modulecheck.parsing.source.McName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.cache.SafeCache
import modulecheck.utils.coroutines.flatMapSetConcat
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.lazySet
import modulecheck.utils.lazy.toLazySet

/**
 * Cache of all [Generated] declarations created by this project, where each [Generated] lists its
 * [sources][Generated.sources].
 *
 * @since 0.13.0
 */
class GeneratedDeclarations private constructor(
  private val project: McProject
) : ProjectContext.Element {

  private val allGenerated: SafeCache<SourceSetName, LazySet<Generated>> by lazy {
    SafeCache(listOf(project.projectPath, GeneratedDeclarations::class))
  }

  /**
   * the map holds all generated declarations for a given
   * [ReferenceName][modulecheck.parsing.source.ReferenceName]. This is more efficient than a linear
   * search/filter of a full list of [Generated] when there will be lookups for more than one
   * source.
   *
   * @since 0.13.0
   */
  private val reversed: SafeCache<SourceSetName, Map<McName, Set<Generated>>> by lazy {
    SafeCache(listOf(project.projectPath, GeneratedDeclarations::class))
  }

  /**
   * Just a cache of the results from filtering all generated for a particular set of sources.
   *
   * @since 0.13.0
   */
  private val filtered: SafeCache<SourceSetWithDeclarations, LazySet<Generated>> by lazy {
    SafeCache(listOf(project.projectPath, GeneratedDeclarations::class))
  }

  private data class SourceSetWithDeclarations(
    val sourceSetName: SourceSetName,
    val sourceDeclarations: LazySet<DeclaredName>
  )

  override val key: ProjectContext.Key<GeneratedDeclarations>
    get() = Key

  /**
   * @return all [Generated] declarations for a given source set
   * @since 0.13.0
   */
  suspend fun get(sourceSetName: SourceSetName): LazySet<Generated> {

    return allGenerated.getOrPut(sourceSetName) {
      project.declarations()
        .get(sourceSetName, includeUpstream = true)
        .filterIsInstance<Generated>()
        .toLazySet()
    }
  }

  /**
   * @return all [Generated] declarations from a given source set which reference a declaration
   *     contained in [sourceDeclarations]. One example would be generated Android resources which
   *     come from another module.
   * @since 0.13.0
   */
  suspend fun get(
    sourceSetName: SourceSetName,
    sourceDeclarations: LazySet<DeclaredName>
  ): LazySet<Generated> =
    filtered.getOrPut(SourceSetWithDeclarations(sourceSetName, sourceDeclarations)) {
      lazySet {
        val cache = sourcesToGenerated(sourceSetName)

        sourceDeclarations
          .flatMapSetConcat { declaredName ->
            cache[declaredName].orEmpty()
          }
      }
    }

  private suspend fun sourcesToGenerated(
    sourceSetName: SourceSetName
  ): Map<McName, Set<Generated>> {
    return reversed.getOrPut(sourceSetName) {

      buildMap<McName, MutableSet<Generated>> {

        project.generatedDeclarations(sourceSetName)
          .collect { generated ->

            generated.sources.forEach { referenceName ->

              get(referenceName)?.add(generated)
                ?: put(referenceName, mutableSetOf(generated))
            }
          }
      }
    }
  }

  /**
   * The [ProjectContext] key for [GeneratedDeclarations].
   *
   * @since 0.13.0
   */
  companion object Key : ProjectContext.Key<GeneratedDeclarations> {
    override suspend operator fun invoke(project: McProject): GeneratedDeclarations {
      return GeneratedDeclarations(project)
    }
  }
}

/**
 * shorthand for `project.get(GeneratedDeclarations)`
 *
 * @since 0.13.0
 */
suspend fun ProjectContext.generatedDeclarations(): GeneratedDeclarations =
  get(GeneratedDeclarations)

/**
 * @return all [Generated] declarations for a given source set
 * @since 0.13.0
 */
suspend fun ProjectContext.generatedDeclarations(
  sourceSetName: SourceSetName
): LazySet<Generated> = generatedDeclarations().get(sourceSetName)

/**
 * @return all [Generated] declarations from a given source set which reference a declaration
 *     contained in [sourceDeclarations]. One example would be generated Android resources which
 *     come from another module.
 * @since 0.13.0
 */
suspend fun ProjectContext.generatedDeclarations(
  sourceSetName: SourceSetName,
  sourceDeclarations: LazySet<DeclaredName>
): LazySet<Generated> = generatedDeclarations().get(sourceSetName, sourceDeclarations)
