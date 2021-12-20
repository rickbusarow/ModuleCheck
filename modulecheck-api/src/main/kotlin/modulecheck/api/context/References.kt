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

import modulecheck.parsing.gradle.SourceSetName
import modulecheck.project.AndroidMcProject
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.SafeCache
import modulecheck.utils.flatMapSetConcat

data class References(
  private val delegate: SafeCache<SourceSetName, Set<ReferenceName>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<References>
    get() = Key

  suspend fun get(sourceSetName: SourceSetName): Set<ReferenceName> {
    return delegate.getOrPut(sourceSetName) { fetchNewReferences(sourceSetName) }
  }

  private suspend fun fetchNewReferences(sourceSetName: SourceSetName): Set<ReferenceName> {

    val androidRFqNameOrNull = (project as? AndroidMcProject)?.androidRFqNameOrNull
    val packagePrefix = (project as? AndroidMcProject)
      ?.androidPackageOrNull
      ?.let { "$it." }

    val jvm = project.get(JvmFiles)
      .get(sourceSetName)
      .flatMapSetConcat { jvmFile ->
        val maybeExtra = jvmFile.maybeExtraReferences.await()

        if (androidRFqNameOrNull == null || packagePrefix == null) {
          return@flatMapSetConcat maybeExtra
        }

        val rReferences = maybeExtra
          .filter { it.startsWith(androidRFqNameOrNull) }
          .plus(jvmFile.imports.filter { it.startsWith(androidRFqNameOrNull) })
          .map { it.removePrefix(packagePrefix) }
          .toSet()

        maybeExtra + rReferences
      }

    val layout = project.get(LayoutFiles)
      .get(sourceSetName)
      .flatMap { it.resourceReferencesAsRReferences }
      .toSet()

    val manifest = project.manifestFileForSourceSetName(sourceSetName)
      ?.resourceReferencesAsRReferences
      .orEmpty()

    return jvm + layout + manifest
  }

  companion object Key : ProjectContext.Key<References> {
    override suspend operator fun invoke(project: McProject): References {

      return References(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.references(): References = get(References)
suspend fun ProjectContext.referencesForSourceSetName(
  sourceSetName: SourceSetName
): Set<String> = references().get(sourceSetName)
