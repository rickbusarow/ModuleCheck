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
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.utils.SafeCache

data class AndroidRFqNames(
  private val delegate: SafeCache<SourceSetName, String?>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<AndroidRFqNames>
    get() = Key

  suspend fun get(sourceSetName: SourceSetName): String? {

    return delegate.getOrPut(sourceSetName) {

      project.androidBasePackagesForSourceSetName(sourceSetName)?.let { "$it.R" }
    }
  }

  companion object Key : ProjectContext.Key<AndroidRFqNames> {
    override suspend operator fun invoke(project: McProject): AndroidRFqNames {

      return AndroidRFqNames(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.androidRFqNames(): AndroidRFqNames =
  get(AndroidRFqNames)

suspend fun ProjectContext.androidRFqNameForSourceSetName(
  sourceSetName: SourceSetName
): String? = androidRFqNames().get(sourceSetName)
