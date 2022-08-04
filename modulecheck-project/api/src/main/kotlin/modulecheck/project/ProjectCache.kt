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

package modulecheck.project

import modulecheck.dagger.SingleIn
import modulecheck.dagger.TaskScope
import modulecheck.model.dependency.ProjectPath
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.model.dependency.ProjectPath.TypeSafeProjectPath
import modulecheck.utils.requireNotNull
import modulecheck.utils.trace.HasTraceTags
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@SingleIn(TaskScope::class)
class ProjectCache @Inject constructor() : HasTraceTags {
  private val delegate = ConcurrentHashMap<ProjectPath, McProject>()

  override val tags = listOf(this::class)

  val values: MutableCollection<McProject> get() = delegate.values

  /**
   * N.B. This [path] argument can be the base [ProjectPath] instead of one of the concrete types
   * ([StringProjectPath], [TypeSafeProjectPath]), because all project paths are compared using the
   * derived type-safe variant. So, there are no cache misses when a project is already stored using
   * the String variant, but then we attempt to look it up via the type-safe one.
   *
   * @since 0.12.0
   */
  fun getOrPut(path: ProjectPath, defaultValue: () -> McProject): McProject {
    return delegate.getOrPut(path, defaultValue)
  }

  fun getValue(path: ProjectPath): McProject {
    return delegate[path].requireNotNull {
      "Expected to find a project with a path of '${path.value}`, but no such project exists.\n\n" +
        "The existing paths are: ${delegate.keys.map { it.value }}"
    }
  }

  operator fun set(path: ProjectPath, project: McProject): McProject? {
    return delegate.put(path, project)
  }

  fun clearContexts() {
    delegate.values.forEach { it.clearContext() }
  }
}
