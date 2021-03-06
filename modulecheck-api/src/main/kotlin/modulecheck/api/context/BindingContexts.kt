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

import modulecheck.api.Project2
import modulecheck.api.SourceSetName
import modulecheck.psi.createBindingContext
import modulecheck.psi.internal.ktFiles
import org.jetbrains.kotlin.resolve.BindingContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class BindingContexts(
  internal val delegate: ConcurrentMap<SourceSetName, BindingContext>
) : ConcurrentMap<SourceSetName, BindingContext> by delegate,
  ProjectContext.Element {

  override val key: ProjectContext.Key<BindingContexts>
    get() = Key

  companion object Key : ProjectContext.Key<BindingContexts> {
    override operator fun invoke(project: Project2): BindingContexts {
      val map = project
        .sourceSets
        .mapValues { (_, sourceSet) ->

          val classPath = project.classpathForSourceSet(sourceSet).map { it.path }
          val jvmSources = project.jvmSourcesForSourceSet(sourceSet).ktFiles()

          createBindingContext(classPath, jvmSources)
        }

      return BindingContexts(ConcurrentHashMap(map))
    }
  }
}
