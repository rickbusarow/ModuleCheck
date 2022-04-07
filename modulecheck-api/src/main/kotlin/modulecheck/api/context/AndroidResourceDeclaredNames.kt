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

import modulecheck.parsing.android.AndroidResourceParser
import modulecheck.parsing.gradle.AndroidPlatformPlugin.AndroidLibraryPlugin
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.gradle.asSourceSetName
import modulecheck.parsing.source.AndroidResourceDeclaredName
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.isAndroid
import modulecheck.utils.LazySet
import modulecheck.utils.SafeCache
import modulecheck.utils.dataSource
import modulecheck.utils.emptyLazySet
import modulecheck.utils.lazySet

data class AndroidResourceDeclaredNames(
  private val delegate: SafeCache<SourceSetName, LazySet<AndroidResourceDeclaredName>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<AndroidResourceDeclaredNames>
    get() = Key

  suspend fun all(): LazySet<AndroidResourceDeclaredName> {
    return delegate.getOrPut("all_source_sets".asSourceSetName()) {
      project.platformPlugin
        .sourceSets
        .keys
        .map { get(it) }
        .let { lazySet(it) }
    }
  }

  suspend fun get(sourceSetName: SourceSetName): LazySet<AndroidResourceDeclaredName> {
    if (!project.isAndroid()) return emptyLazySet()

    val platformPlugin = project.platformPlugin

    if (platformPlugin is AndroidLibraryPlugin && !platformPlugin.androidResourcesEnabled) {
      return emptyLazySet()
    }

    val rName = project.androidRDeclaredNameForSourceSetName(sourceSetName)
      ?: return emptyLazySet()

    return delegate.getOrPut(sourceSetName) {

      val layoutsAndIds = project.layoutFilesForSourceSetName(sourceSetName)
        .map { layoutFile ->

          dataSource {
            layoutFile.idDeclarations
              .plus(UnqualifiedAndroidResourceDeclaredName.fromFile(layoutFile.file))
              .filterNotNull()
              .toSet()
          }
        }

      val declarations = project.resourceFilesForSourceSetName(sourceSetName)
        .map { file ->
          dataSource {
            val simpleNames = AndroidResourceParser().parseFile(file)

            simpleNames + simpleNames.map { it.toNamespacedDeclaredName(rName) }
          }
        }
        .plus(layoutsAndIds)
        .plus(project.androidDataBindingDeclarationsForSourceSetName(sourceSetName))

      lazySet(declarations)
    }
  }

  companion object Key : ProjectContext.Key<AndroidResourceDeclaredNames> {
    override suspend operator fun invoke(project: McProject): AndroidResourceDeclaredNames {
      return AndroidResourceDeclaredNames(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.androidResourceDeclaredNames(): AndroidResourceDeclaredNames =
  get(AndroidResourceDeclaredNames)

suspend fun ProjectContext.androidResourceDeclaredNamesForSourceSetName(
  sourceSetName: SourceSetName
): LazySet<AndroidResourceDeclaredName> {
  return androidResourceDeclaredNames().get(sourceSetName)
}
