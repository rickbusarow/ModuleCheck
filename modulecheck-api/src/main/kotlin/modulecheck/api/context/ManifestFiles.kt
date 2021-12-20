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

import modulecheck.parsing.android.XmlFile
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.project.AndroidMcProject
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.SafeCache
import modulecheck.utils.existsOrNull

data class ManifestFiles(
  private val delegate: SafeCache<SourceSetName, XmlFile.ManifestFile?>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<ManifestFiles>
    get() = Key

  suspend fun get(sourceSetName: SourceSetName): XmlFile.ManifestFile? {
    if (project !is AndroidMcProject) return null

    return delegate.getOrPut(sourceSetName) {
      val file = project.manifests[sourceSetName]
        ?.existsOrNull()
        ?: return@getOrPut null

      XmlFile.ManifestFile(file)
    }
  }

  companion object Key : ProjectContext.Key<ManifestFiles> {
    override suspend operator fun invoke(project: McProject): ManifestFiles {

      return ManifestFiles(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.manifestFiles(): ManifestFiles = get(ManifestFiles)

suspend fun ProjectContext.manifestFileForSourceSetName(
  sourceSetName: SourceSetName
): XmlFile.ManifestFile? = manifestFiles()
  .get(sourceSetName)
  ?.takeIf { it.file.exists() }
  ?: manifestFiles().get(SourceSetName.MAIN)
    ?.takeIf { it.file.exists() }
