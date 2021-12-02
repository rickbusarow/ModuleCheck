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

import modulecheck.project.AndroidMcProject
import modulecheck.project.DeclarationName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.SourceSetName
import modulecheck.project.asDeclarationName
import modulecheck.utils.SafeCache
import modulecheck.utils.capitalize
import java.util.Locale

data class ViewBindingFiles(
  private val delegate: SafeCache<SourceSetName, Set<DeclarationName>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<ViewBindingFiles>
    get() = Key

  suspend fun get(sourceSetName: SourceSetName): Set<DeclarationName> {

    if (project !is AndroidMcProject) return emptySet()

    val basePackage = project.androidPackageOrNull ?: return emptySet()

    return delegate.getOrPut(sourceSetName) {

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
  }

  companion object Key : ProjectContext.Key<ViewBindingFiles> {

    private val snake_reg = "_([a-zA-Z])".toRegex()

    override suspend operator fun invoke(project: McProject): ViewBindingFiles {

      return ViewBindingFiles(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.viewBindingFiles(): ViewBindingFiles = get(ViewBindingFiles)
suspend fun ProjectContext.viewBindingFilesForSourceSetName(
  sourceSetName: SourceSetName
): Set<DeclarationName> = viewBindingFiles().get(sourceSetName)
