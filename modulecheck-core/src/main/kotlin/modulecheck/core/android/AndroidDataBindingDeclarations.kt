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

package modulecheck.core.android

import modulecheck.api.context.ResSourceFiles
import modulecheck.parsing.AndroidMcProject
import modulecheck.parsing.DeclarationName
import modulecheck.parsing.McProject
import modulecheck.parsing.ProjectContext
import modulecheck.parsing.SourceSetName
import modulecheck.parsing.asDeclarationName
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class AndroidDataBindingDeclarations(
  internal val delegate: ConcurrentMap<SourceSetName, Set<DeclarationName>>
) : ConcurrentMap<SourceSetName, Set<DeclarationName>> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<AndroidDataBindingDeclarations>
    get() = Key

  companion object Key : ProjectContext.Key<AndroidDataBindingDeclarations> {

    private val filterReg =
      """.*\${java.io.File.separator}layout.*\${java.io.File.separator}.*.xml""".toRegex()

    override suspend operator fun invoke(project: McProject): AndroidDataBindingDeclarations {

      val android = project as? AndroidMcProject
        ?: return AndroidDataBindingDeclarations(ConcurrentHashMap())

      val basePackage = android.androidPackageOrNull
        ?: return AndroidDataBindingDeclarations(ConcurrentHashMap())

      val map = project
        .sourceSets
        .mapValues { (sourceSetName, _) ->

          project.get(ResSourceFiles)[sourceSetName]
            .orEmpty()
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

      return AndroidDataBindingDeclarations(ConcurrentHashMap(map))
    }
  }
}

suspend fun ProjectContext.androidDataBindingDeclarations(): AndroidDataBindingDeclarations =
  get(AndroidDataBindingDeclarations)

suspend fun ProjectContext.androidDataBindingDeclarationsForSourceSetName(
  sourceSetName: SourceSetName
): Set<DeclarationName> {
  return get(AndroidDataBindingDeclarations)[sourceSetName].orEmpty()
}
