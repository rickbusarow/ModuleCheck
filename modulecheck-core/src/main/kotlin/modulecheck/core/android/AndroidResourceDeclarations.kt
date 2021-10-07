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

import modulecheck.api.AndroidProject2
import modulecheck.api.Project2
import modulecheck.api.SourceSetName
import modulecheck.api.context.JvmFiles
import modulecheck.api.context.ProjectContext
import modulecheck.api.context.ResSourceFiles
import modulecheck.parsing.DeclarationName
import modulecheck.parsing.asDeclaractionName
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
    override operator fun invoke(project: Project2): AndroidResourceDeclarations {
      val android = project as? AndroidProject2
        ?: return AndroidResourceDeclarations(ConcurrentHashMap())

      val rPackage = android.androidPackageOrNull

      val map = project
        .sourceSets
        .mapValues { (sourceSetName, _) ->

          if (rPackage != null) {
            project[ResSourceFiles][sourceSetName]
              .orEmpty()
              .flatMap { AndroidResourceParser.parseFile(it) }
              .toSet() + "$rPackage.R".asDeclaractionName()
          } else {
            project[JvmFiles][sourceSetName]
              .orEmpty()
              .flatMap { it.declarations }
              .toSet()
          }
        }

      return AndroidResourceDeclarations(ConcurrentHashMap(map))
    }
  }
}

val ProjectContext.androidResourceDeclarations: AndroidResourceDeclarations
  get() = get(AndroidResourceDeclarations)

fun ProjectContext.androidResourceDeclarationsForSourceSetName(
  sourceSetName: SourceSetName
): Set<DeclarationName> {
  return get(AndroidResourceDeclarations)[sourceSetName].orEmpty()
}
