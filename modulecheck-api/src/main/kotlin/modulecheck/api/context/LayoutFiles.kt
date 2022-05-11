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

import modulecheck.parsing.android.XmlFile
import modulecheck.parsing.android.XmlFile.LayoutFile
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.SafeCache

data class LayoutFiles(
  private val delegate: SafeCache<SourceSetName, Set<LayoutFile>>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<LayoutFiles>
    get() = Key

  suspend fun all(): Map<SourceSetName, Set<LayoutFile>> {
    return project.sourceSets
      .mapValues { (sourceSetName, _) ->
        get(sourceSetName)
      }
  }

  suspend fun get(sourceSetName: SourceSetName): Set<LayoutFile> {

    return delegate.getOrPut(sourceSetName) {

      val sourceSet = project.sourceSets[sourceSetName] ?: return@getOrPut emptySet()
      sourceSet
        .layoutFiles
        .map { XmlFile.LayoutFile(it) }
        .toSet()
    }
  }

  companion object Key : ProjectContext.Key<LayoutFiles> {
    override suspend operator fun invoke(project: McProject): LayoutFiles {

      return LayoutFiles(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.layoutFiles(): LayoutFiles = get(LayoutFiles)
suspend fun ProjectContext.layoutFilesForSourceSetName(
  sourceSetName: SourceSetName
): Set<XmlFile.LayoutFile> = layoutFiles().get(sourceSetName)
