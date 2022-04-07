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
import modulecheck.parsing.source.AndroidDataBindingDeclaredName
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.isAndroid
import modulecheck.utils.LazySet
import modulecheck.utils.SafeCache
import modulecheck.utils.capitalize
import modulecheck.utils.dataSource
import modulecheck.utils.emptyLazySet
import modulecheck.utils.existsOrNull
import modulecheck.utils.mapToSet
import modulecheck.utils.toLazySet

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

                val layoutDeclaration = UnqualifiedAndroidResourceDeclaredName
                  .Layout(layoutFile.nameWithoutExtension)

                val simpleBindingName = layoutFile.nameWithoutExtension
                  .split("_")
                  .joinToString("") { fragment -> fragment.capitalize() } + "Binding"

                // fully qualified
                AndroidDataBindingDeclaredName(
                  name = "$basePackage.databinding.$simpleBindingName",
                  sourceLayout = layoutDeclaration
                )
              }
          }
        }.toLazySet()
    }
  }

  companion object Key : ProjectContext.Key<AndroidDataBindingDeclarations> {
    override suspend operator fun invoke(project: McProject): AndroidDataBindingDeclarations {

      return AndroidDataBindingDeclarations(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.androidDataBindingDeclarations(): AndroidDataBindingDeclarations =
  get(AndroidDataBindingDeclarations)

suspend fun ProjectContext.androidDataBindingDeclarationsForSourceSetName(
  sourceSetName: SourceSetName
): LazySet<AndroidDataBindingDeclaredName> = androidDataBindingDeclarations().get(sourceSetName)
