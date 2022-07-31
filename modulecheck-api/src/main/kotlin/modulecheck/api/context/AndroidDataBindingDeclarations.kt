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

import modulecheck.model.dependency.withUpstream
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.source.AndroidDataBindingDeclaredName
import modulecheck.parsing.source.AndroidResourceDeclaredName
import modulecheck.parsing.source.SimpleName.Companion.asSimpleName
import modulecheck.parsing.source.UnqualifiedAndroidResource
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.isAndroid
import modulecheck.utils.cache.SafeCache
import modulecheck.utils.existsOrNull
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.dataSource
import modulecheck.utils.lazy.emptyLazySet
import modulecheck.utils.lazy.toLazySet
import modulecheck.utils.mapToSet

data class AndroidDataBindingDeclarations(
  private val delegate: SafeCache<SourceSetName, LazySet<AndroidDataBindingDeclaredName>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<AndroidDataBindingDeclarations>
    get() = Key

  suspend fun get(sourceSetName: SourceSetName): LazySet<AndroidDataBindingDeclaredName> {
    if (!project.isAndroid()) return emptyLazySet()

    return delegate.getOrPut(sourceSetName) {

      val basePackage = project.androidBasePackagesForSourceSetName(sourceSetName)
        ?: return@getOrPut emptyLazySet()

      sourceSetName
        .withUpstream(project)
        .map { sourceSetOrUpstream ->

          dataSource {
            project.layoutFilesForSourceSetName(sourceSetOrUpstream)
              .mapNotNull { it.file.existsOrNull() }
              .mapToSet { layoutFile ->

                val layoutDeclaration = UnqualifiedAndroidResource
                  .layout(layoutFile.nameWithoutExtension.asSimpleName())

                // fully qualified
                AndroidResourceDeclaredName.dataBinding(
                  sourceLayoutDeclaration = layoutDeclaration,
                  packageName = basePackage
                )
              }
          }
        }.toLazySet()
    }
  }

  companion object Key : ProjectContext.Key<AndroidDataBindingDeclarations> {
    override suspend operator fun invoke(project: McProject): AndroidDataBindingDeclarations {

      return AndroidDataBindingDeclarations(
        SafeCache(listOf(project.path, AndroidDataBindingDeclarations::class)),
        project
      )
    }
  }
}

suspend fun ProjectContext.androidDataBindingDeclarations(): AndroidDataBindingDeclarations =
  get(AndroidDataBindingDeclarations)

suspend fun ProjectContext.androidDataBindingDeclarationsForSourceSetName(
  sourceSetName: SourceSetName
): LazySet<AndroidDataBindingDeclaredName> = androidDataBindingDeclarations().get(sourceSetName)
