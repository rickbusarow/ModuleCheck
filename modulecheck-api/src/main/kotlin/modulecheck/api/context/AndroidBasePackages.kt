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

import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.isAndroid
import modulecheck.utils.cache.SafeCache

data class AndroidBasePackages(
  private val delegate: SafeCache<SourceSetName, String?>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<AndroidBasePackages>
    get() = Key

  suspend fun get(sourceSetName: SourceSetName): String? {
    if (!project.isAndroid()) return null

    return delegate.getOrPut(sourceSetName) {

      // Note that this isn't just looking for a manifest file.  It's looking for a manifest which
      // has a defined base package.  It's possible for a manifest to exist, but just add an
      // Activity or something, if the package is already defined in an withUpstream source set.
      sourceSetName
        .withUpstream(project)
        .firstNotNullOfOrNull { sourceSetOrUpstream ->

          project.manifestFileForSourceSetName(sourceSetOrUpstream)?.basePackage
        }
    }
  }

  companion object Key : ProjectContext.Key<AndroidBasePackages> {
    override suspend operator fun invoke(project: McProject): AndroidBasePackages {

      return AndroidBasePackages(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.androidBasePackages(): AndroidBasePackages =
  get(AndroidBasePackages)

suspend fun ProjectContext.androidBasePackagesForSourceSetName(
  sourceSetName: SourceSetName
): String? = androidBasePackages().get(sourceSetName)
