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

import modulecheck.dagger.AppScope
import modulecheck.dagger.SingleIn
import modulecheck.parsing.gradle.ProjectPath
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@SingleIn(AppScope::class)
class ProjectCache @Inject constructor() {
  private val delegate = ConcurrentHashMap<ProjectPath, McProject>()

  val values: MutableCollection<McProject> get() = delegate.values

  fun getOrPut(path: ProjectPath, defaultValue: () -> McProject): McProject {
    return delegate.getOrPut(path, defaultValue)
  }

  fun getValue(path: ProjectPath): McProject {
    return delegate.getValue(path)
  }

  operator fun set(path: ProjectPath, project: McProject): McProject? {
    return delegate.put(path, project)
  }

  fun clearContexts() {
    delegate.values.forEach { it.clearContext() }
  }
}
