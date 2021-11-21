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

import modulecheck.api.context.JvmFiles
import modulecheck.api.context.ResSourceFiles
import modulecheck.parsing.AndroidMcProject
import modulecheck.parsing.DeclarationName
import modulecheck.parsing.McProject
import modulecheck.parsing.ProjectContext
import modulecheck.parsing.SourceSetName
import modulecheck.parsing.asDeclarationName
import modulecheck.parsing.xml.AndroidResourceParser
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class AndroidResourceDeclarations(
  internal val delegate: ConcurrentMap<SourceSetName, Set<DeclarationName>>
) : ConcurrentMap<SourceSetName, Set<DeclarationName>> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<AndroidResourceDeclarations>
    get() = Key

  companion object Key : ProjectContext.Key<AndroidResourceDeclarations> {
    override suspend operator fun invoke(project: McProject): AndroidResourceDeclarations {
      val android = project as? AndroidMcProject
        ?: return AndroidResourceDeclarations(ConcurrentHashMap())

      val rPackage = android.androidPackageOrNull

      val resourceParser = AndroidResourceParser()

      val map = project
        .sourceSets
        .mapValues { (sourceSetName, _) ->

          if (rPackage != null) {
            project.get(ResSourceFiles)[sourceSetName]
              .orEmpty()
              .flatMap { resourceParser.parseFile(it) }
              .toSet() + "$rPackage.R".asDeclarationName()
          } else {
            project.get(JvmFiles)[sourceSetName]
              .orEmpty()
              .flatMap { it.declarations }
              .toSet()
          }
        }

      return AndroidResourceDeclarations(ConcurrentHashMap(map))
    }
  }
}

suspend fun ProjectContext.androidResourceDeclarations(): AndroidResourceDeclarations =
  get(AndroidResourceDeclarations)

suspend fun ProjectContext.androidResourceDeclarationsForSourceSetName(
  sourceSetName: SourceSetName
): Set<DeclarationName> {
  return get(AndroidResourceDeclarations)[sourceSetName].orEmpty()
}
