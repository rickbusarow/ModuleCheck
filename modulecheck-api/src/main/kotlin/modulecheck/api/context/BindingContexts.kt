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

import modulecheck.parsing.psi.createBindingContext
import modulecheck.parsing.psi.internal.ktFiles
import modulecheck.project.McProject
import modulecheck.project.ProjectContext
import modulecheck.project.SourceSetName
import modulecheck.utils.SafeCache
import org.jetbrains.kotlin.resolve.BindingContext

data class BindingContexts(
  private val delegate: SafeCache<SourceSetName, BindingContext>,
  private val project: McProject
) : ProjectContext.Element {

  override val key: ProjectContext.Key<BindingContexts>
    get() = Key

  suspend fun get(sourceSetName: SourceSetName): BindingContext {
    return delegate.getOrPut(sourceSetName) {

      val classPath = project
        .classpathForSourceSetName(sourceSetName)
        .map { it.path }
      val jvmSources = project
        .jvmSourcesForSourceSetName(sourceSetName)
        .ktFiles()

      createBindingContext(classPath, jvmSources)
    }
  }

  companion object Key : ProjectContext.Key<BindingContexts> {
    override suspend operator fun invoke(project: McProject): BindingContexts {

      return BindingContexts(SafeCache(), project)
    }
  }
}

suspend fun ProjectContext.bindingContexts(): BindingContexts = get(BindingContexts)
suspend fun ProjectContext.bindingContextForSourceSetName(
  sourceSetName: SourceSetName
): BindingContext = bindingContexts().get(sourceSetName)
