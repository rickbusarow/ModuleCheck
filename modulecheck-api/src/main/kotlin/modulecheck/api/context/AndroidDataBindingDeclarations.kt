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
import modulecheck.parsing.source.DeclarationName
import modulecheck.parsing.source.asDeclarationName
import modulecheck.project.AndroidMcProject
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.LazySet
import modulecheck.utils.SafeCache
import modulecheck.utils.capitalize
import modulecheck.utils.emptyLazySet
import modulecheck.utils.lazyDataSource
import modulecheck.utils.lazySet
import java.util.Locale

data class AndroidDataBindingDeclarations(
  private val delegate: SafeCache<SourceSetName, LazySet<DeclarationName>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<AndroidDataBindingDeclarations>
    get() = Key

  suspend fun get(sourceSetName: SourceSetName): LazySet<DeclarationName> {

    if (project !is AndroidMcProject) return emptyLazySet()

    return delegate.getOrPut(sourceSetName) {

      val basePackage = project.androidBasePackagesForSourceSetName(sourceSetName)
        ?: return@getOrPut emptyLazySet()

      lazySet(
        lazyDataSource {
          project.layoutFiles()
            .get(sourceSetName)
            .map { layoutFile ->
              layoutFile.name
                .capitalize(Locale.US)
                .replace(snake_reg) { matchResult ->
                  matchResult.destructured
                    .component1()
                    .uppercase()
                }
                .plus("Binding")
                .let { viewBindingName -> "$basePackage.databinding.$viewBindingName" }
                .asDeclarationName()
            }
            .toSet()
        }
      )
    }
  }

  companion object Key : ProjectContext.Key<AndroidDataBindingDeclarations> {

    private val snake_reg = "_([a-zA-Z])".toRegex()

    override suspend operator fun invoke(project: McProject): AndroidDataBindingDeclarations {

      return AndroidDataBindingDeclarations(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.androidDataBindingDeclarations(): AndroidDataBindingDeclarations =
  get(AndroidDataBindingDeclarations)

suspend fun ProjectContext.androidDataBindingDeclarationsForSourceSetName(
  sourceSetName: SourceSetName
): LazySet<DeclarationName> = androidDataBindingDeclarations().get(sourceSetName)
