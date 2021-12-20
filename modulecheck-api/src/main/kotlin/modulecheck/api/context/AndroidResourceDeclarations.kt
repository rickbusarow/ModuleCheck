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

import modulecheck.parsing.android.AndroidResourceParser
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.source.DeclarationName
import modulecheck.parsing.source.asDeclarationName
import modulecheck.project.AndroidMcProject
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.SafeCache
import modulecheck.utils.flatMapSetConcat

data class AndroidResourceDeclarations(
  private val delegate: SafeCache<SourceSetName, Set<DeclarationName>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<AndroidResourceDeclarations>
    get() = Key

  suspend fun get(sourceSetName: SourceSetName): Set<DeclarationName> {

    return delegate.getOrPut(sourceSetName) {

      val android = project as? AndroidMcProject
        ?: return@getOrPut emptySet()

      val rName = android.androidRFqNameOrNull

      val resourceParser = AndroidResourceParser()

      if (rName != null) {
        project.resourcesForSourceSetName(sourceSetName)
          .flatMap { resourceParser.parseFile(it) }
          .toSet() + rName.asDeclarationName()
      } else {
        project.get(JvmFiles)
          .get(sourceSetName)
          .flatMapSetConcat { it.declarations }
      }
    }
  }

  companion object Key : ProjectContext.Key<AndroidResourceDeclarations> {
    override suspend operator fun invoke(project: McProject): AndroidResourceDeclarations {

      return AndroidResourceDeclarations(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.androidResourceDeclarations(): AndroidResourceDeclarations =
  get(AndroidResourceDeclarations)

suspend fun ProjectContext.androidResourceDeclarationsForSourceSetName(
  sourceSetName: SourceSetName
): Set<DeclarationName> {
  return androidResourceDeclarations().get(sourceSetName)
}
