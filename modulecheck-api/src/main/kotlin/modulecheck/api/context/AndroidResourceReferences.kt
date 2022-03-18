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

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.source.Reference
import modulecheck.parsing.source.Reference.ExplicitJavaReference
import modulecheck.parsing.source.Reference.UnqualifiedJavaAndroidResourceReference
import modulecheck.project.AndroidMcProject
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.LazySet
import modulecheck.utils.LazySet.DataSource
import modulecheck.utils.LazySet.DataSource.Priority.LOW
import modulecheck.utils.SafeCache
import modulecheck.utils.asDataSource
import modulecheck.utils.emptyLazySet
import modulecheck.utils.lazySet

data class AndroidResourceReferences(
  private val delegate: SafeCache<SourceSetName, LazySet<Reference>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<AndroidResourceReferences>
    get() = Key

  suspend fun get(sourceSetName: SourceSetName): LazySet<Reference> {
    return delegate.getOrPut(sourceSetName) { fetchNewReferences(sourceSetName) }
  }

  private suspend fun fetchNewReferences(sourceSetName: SourceSetName): LazySet<Reference> {

    val androidRFqNameOrNull = (project as? AndroidMcProject)
      ?.androidRFqNameForSourceSetName(sourceSetName)
      ?: return emptyLazySet()

    val packagePrefix = (project as? AndroidMcProject)
      ?.androidBasePackagesForSourceSetName(sourceSetName)
      ?.let { "$it." }
      ?: return emptyLazySet()

    val jvm: List<DataSource<Reference>> = project.jvmFilesForSourceSetName(sourceSetName)
      .map { jvmFile ->

        lazy<Set<Reference>> {
          jvmFile.interpretedReferencesLazy
            .value
            .asSequence()
            .flatMap { reference -> reference.startingWith(androidRFqNameOrNull) }
            .plus(jvmFile.importsLazy.value.flatMap { it.startingWith(androidRFqNameOrNull) })
            .flatMap { rReference ->

              listOf(
                UnqualifiedJavaAndroidResourceReference(rReference.removePrefix(packagePrefix)),
                ExplicitJavaReference(rReference)
              )
            }
            .toSet()
        }.asDataSource(LOW)
      }
      .toList()

    val layout = project.layoutFilesForSourceSetName(sourceSetName)
      .flatMap { it.references() }

    val manifest = project.manifestFileForSourceSetName(sourceSetName)
      ?.references()

    val all = if (manifest != null) {
      jvm + layout + manifest
    } else {
      jvm + layout
    }

    return lazySet(listOf(), all)
  }

  companion object Key : ProjectContext.Key<AndroidResourceReferences> {
    override suspend operator fun invoke(project: McProject): AndroidResourceReferences {

      return AndroidResourceReferences(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.androidResourceReferencesForSourceSetName(
  sourceSetName: SourceSetName
): LazySet<Reference> = get(AndroidResourceReferences).get(sourceSetName)
