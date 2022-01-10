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
import modulecheck.utils.flatMapSetConcat

data class Declarations(
  private val delegate: SafeCache<SourceSetName, Set<DeclarationName>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<Declarations>
    get() = Key

  suspend fun get(sourceSetName: SourceSetName): Set<DeclarationName> {
    return delegate.getOrPut(sourceSetName) {

      val jvmFiles = project.jvmFilesForSourceSetName(sourceSetName)
        .flatMapSetConcat { jvmFile -> jvmFile.declarations }

      val rName = (project as? AndroidMcProject)?.androidRFqNameOrNull
        ?: return@getOrPut jvmFiles

      jvmFiles
        .plus(rName.asDeclarationName())
        .plus(project.viewBindingFilesForSourceSetName(sourceSetName))
        .plus(project.androidResourceDeclarationsForSourceSetName(sourceSetName))
    }
  }

  companion object Key : ProjectContext.Key<Declarations> {
    override suspend operator fun invoke(project: McProject): Declarations {

      return Declarations(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.declarations(): Declarations = get(Declarations)
