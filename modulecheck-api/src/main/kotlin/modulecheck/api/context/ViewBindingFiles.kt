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

import modulecheck.parsing.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class ViewBindingFiles(
  internal val delegate: ConcurrentMap<SourceSetName, Set<DeclarationName>>
) : ConcurrentMap<SourceSetName, Set<DeclarationName>> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<ViewBindingFiles>
    get() = Key

  companion object Key : ProjectContext.Key<ViewBindingFiles> {

    private val snake_reg = "_([a-zA-Z])".toRegex()

    override suspend operator fun invoke(project: McProject): ViewBindingFiles {

      if (project !is AndroidMcProject) return ViewBindingFiles(ConcurrentHashMap())

      val basePackage = project.androidPackageOrNull ?: return ViewBindingFiles(ConcurrentHashMap())

      val map = project.layoutFiles()
        .mapValues { (_, layoutFiles) ->
          layoutFiles
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

      return ViewBindingFiles(ConcurrentHashMap(map))
    }
  }
}

suspend fun ProjectContext.viewBindingFiles(): ViewBindingFiles = get(ViewBindingFiles)
suspend fun ProjectContext.viewBindingFilesForSourceSetName(
  sourceSetName: SourceSetName
): Set<DeclarationName> = viewBindingFiles()[sourceSetName].orEmpty()
