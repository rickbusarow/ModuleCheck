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

import modulecheck.model.dependency.AndroidPlatformPlugin
import modulecheck.model.dependency.withUpstream
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.android.AndroidResourceParser
import modulecheck.parsing.source.UnqualifiedAndroidResource
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.cache.SafeCache
import modulecheck.utils.flatMapToSet
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.dataSource
import modulecheck.utils.lazy.emptyLazySet
import modulecheck.utils.lazy.lazySet
import modulecheck.utils.lazy.toLazySet

/**
 * the cache of [UnqualifiedAndroidResource] for this [project][ProjectContext]
 *
 * @since 0.12.0
 */
data class AndroidUnqualifiedResources(
  private val delegate: SafeCache<SourceSetName, LazySet<UnqualifiedAndroidResource>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<AndroidUnqualifiedResources>
    get() = Key

  /**
   * @return every [UnqualifiedAndroidResource] declared
   *   within this [sourceSetName], like `R.string.app_name`
   * @since 0.12.0
   */
  suspend fun get(sourceSetName: SourceSetName): LazySet<UnqualifiedAndroidResource> {
    val platformPlugin = project.platformPlugin as? AndroidPlatformPlugin
      ?: return emptyLazySet()

    return delegate.getOrPut(sourceSetName) {

      sourceSetName
        .withUpstream(project)
        .map { sourceSetOrUpstream ->

          val layoutsAndIds = project
            .layoutFilesForSourceSetName(sourceSetOrUpstream)
            .map { layoutFile ->

              dataSource {
                layoutFile.idDeclarations
                  .plus(UnqualifiedAndroidResource.fromFile(layoutFile.file))
                  .filterNotNull()
                  .toSet()
              }
            }

          val resValues = dataSource {
            sourceSetName.withUpstream(project)
              .flatMapToSet { sourceSetOrUpstream ->
                platformPlugin.resValues[sourceSetOrUpstream].orEmpty()
              }
          }

          val declarations = project
            .resourceFilesForSourceSetName(sourceSetOrUpstream)
            .map { file ->
              dataSource {

                AndroidResourceParser().parseFile(file)
              }
            }
            .plus(layoutsAndIds)
            .plus(resValues)

          lazySet(declarations)
        }.toLazySet()
    }
  }

  companion object Key : ProjectContext.Key<AndroidUnqualifiedResources> {
    override suspend operator fun invoke(project: McProject): AndroidUnqualifiedResources {

      return AndroidUnqualifiedResources(
        SafeCache(listOf(project.projectPath, AndroidUnqualifiedResources::class)),
        project
      )
    }
  }
}

/**
 * @return the cache of [UnqualifiedAndroidResource] for this [project][ProjectContext]
 * @since 0.12.0
 */
suspend fun ProjectContext.androidUnqualifiedResources(): AndroidUnqualifiedResources =
  get(AndroidUnqualifiedResources)

/**
 * @return every [UnqualifiedAndroidResource] declared
 *   within this [sourceSetName], like `R.string.app_name`
 * @since 0.12.0
 */
suspend fun ProjectContext.androidUnqualifiedResourcesForSourceSetName(
  sourceSetName: SourceSetName
): LazySet<UnqualifiedAndroidResource> = androidUnqualifiedResources().get(sourceSetName)
