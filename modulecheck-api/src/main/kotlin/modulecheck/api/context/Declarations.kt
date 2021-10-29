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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class Declarations(
  internal val delegate: ConcurrentMap<SourceSetName, Set<DeclarationName>>
) : ConcurrentMap<SourceSetName, Set<DeclarationName>> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<Declarations>
    get() = Key

  companion object Key : ProjectContext.Key<Declarations> {
    override operator fun invoke(project: McProject): Declarations {
      val map = project
        .sourceSets
        .mapValues { (sourceSetName, _) ->

          val jvmFiles = project.jvmFilesForSourceSetName(sourceSetName)
            .flatMap { jvmFile -> jvmFile.declarations }
            .toSet()

          val baseAndroidPackage = (project as? AndroidMcProject)?.androidPackageOrNull
            ?: return@mapValues jvmFiles

          jvmFiles
            .plus("$baseAndroidPackage.R".asDeclarationName())
            .plus(project.viewBindingFilesForSourceSetName(sourceSetName))
        }

      return Declarations(ConcurrentHashMap(map))
    }
  }
}

val ProjectContext.declarations: Declarations get() = get(Declarations)
