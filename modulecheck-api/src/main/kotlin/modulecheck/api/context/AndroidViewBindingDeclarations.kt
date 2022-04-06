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

import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.asDeclaredName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.isAndroid
import modulecheck.utils.LazySet
import modulecheck.utils.SafeCache
import modulecheck.utils.capitalize
import modulecheck.utils.dataSource
import modulecheck.utils.emptyLazySet
import modulecheck.utils.existsOrNull
import modulecheck.utils.lazySet
import modulecheck.utils.mapToSet

data class AndroidViewBindingDeclarations(
  private val delegate: SafeCache<SourceSetName, LazySet<DeclaredName>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<AndroidViewBindingDeclarations>
    get() = Key

  suspend fun get(sourceSetName: SourceSetName): LazySet<DeclaredName> {
    if (!project.isAndroid()) return emptyLazySet()

    return delegate.getOrPut(sourceSetName) {

      val basePackage = project.androidBasePackagesForSourceSetName(sourceSetName)
        ?: return@getOrPut emptyLazySet()

      lazySet(
        dataSource {
          project.layoutFilesForSourceSetName(sourceSetName)
            .mapNotNull { it.file.existsOrNull() }
            .mapToSet { layoutFile ->
              val simpleBindingName = layoutFile.nameWithoutExtension
                .split("_")
                .joinToString("") { fragment -> fragment.capitalize() } + "Binding"

              // fully qualified
              "$basePackage.databinding.$simpleBindingName".asDeclaredName()
            }
        }
      )
    }
  }

  companion object Key : ProjectContext.Key<AndroidViewBindingDeclarations> {
    override suspend operator fun invoke(project: McProject): AndroidViewBindingDeclarations {

      return AndroidViewBindingDeclarations(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.androidViewBindingDeclarations(): AndroidViewBindingDeclarations =
  get(AndroidViewBindingDeclarations)

suspend fun ProjectContext.androidViewBindingDeclarationsForSourceSetName(
  sourceSetName: SourceSetName
): LazySet<DeclaredName> = androidViewBindingDeclarations().get(sourceSetName)
