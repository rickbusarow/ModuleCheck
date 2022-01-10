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
import modulecheck.utils.SafeCache
import java.util.Locale

data class AndroidDataBindingDeclarations(
  private val delegate: SafeCache<SourceSetName, Set<DeclarationName>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<AndroidDataBindingDeclarations>
    get() = Key

  suspend fun get(sourceSetName: SourceSetName): Set<DeclarationName> {

    return delegate.getOrPut(sourceSetName) {

      val android = project as? AndroidMcProject
        ?: return@getOrPut emptySet()

      val basePackage = android.androidPackageOrNull
        ?: return@getOrPut emptySet()

      project.resourcesForSourceSetName(sourceSetName)
        .asSequence()
        .filter { it.exists() }
        .filter { it.path.matches(filterReg) }
        .map { file ->

          val generated = file
            .nameWithoutExtension
            .split("_")
            .joinToString("") { fragment ->
              fragment.replaceFirstChar {
                if (it.isLowerCase()) {
                  it.titlecase(Locale.getDefault())
                } else {
                  it.toString()
                }
              }
            } + "Binding"

          "$basePackage.databinding.$generated".asDeclarationName()
        }
        .toSet()
    }
  }

  companion object Key : ProjectContext.Key<AndroidDataBindingDeclarations> {

    private val filterReg =
      """.*\${java.io.File.separator}layout.*\${java.io.File.separator}.*.xml""".toRegex()

    override suspend operator fun invoke(project: McProject): AndroidDataBindingDeclarations {

      return AndroidDataBindingDeclarations(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.androidDataBindingDeclarations(): AndroidDataBindingDeclarations =
  get(AndroidDataBindingDeclarations)

suspend fun ProjectContext.androidDataBindingDeclarationsForSourceSetName(
  sourceSetName: SourceSetName
): Set<DeclarationName> {
  return get(AndroidDataBindingDeclarations).get(sourceSetName)
}
