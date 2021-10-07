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

import modulecheck.api.AndroidProject2
import modulecheck.api.Project2
import modulecheck.api.SourceSetName
import modulecheck.parsing.DeclarationName
import modulecheck.parsing.asDeclaractionName
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class Declarations(
  internal val delegate: ConcurrentMap<SourceSetName, Set<DeclarationName>>
) : ConcurrentMap<SourceSetName, Set<DeclarationName>> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<Declarations>
    get() = Key

  companion object Key : ProjectContext.Key<Declarations> {
    override operator fun invoke(project: Project2): Declarations {
      val map = project
        .sourceSets
        .mapValues { (_, sourceSet) ->

          val rPackage = (project as? AndroidProject2)?.androidPackageOrNull

          val set = if (rPackage != null) {
            project[JvmFiles][sourceSet.name]
              .orEmpty()
              .flatMap { jvmFile -> jvmFile.declarations }
              .toSet() + "$rPackage.R".asDeclaractionName()
          } else {
            project[JvmFiles][sourceSet.name]
              .orEmpty()
              .flatMap { jvmFile -> jvmFile.declarations }
              .toSet()
          }
          set
        }

      return Declarations(ConcurrentHashMap(map))
    }
  }
}

val ProjectContext.declarations: Declarations get() = get(Declarations)
