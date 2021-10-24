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

package modulecheck.parsing

import modulecheck.parsing.ProjectContext.Element
import modulecheck.parsing.ProjectContext.Key
import java.util.concurrent.ConcurrentHashMap

interface ProjectContext {
  operator fun <E : Element> get(key: Key<E>): E

  interface Key<E : Element> {
    operator fun invoke(project: Project2): E
  }

  interface Element {
    val key: Key<*>
  }
}

class ProjectContextImpl(val project: Project2) : ProjectContext {

  private val cache = ConcurrentHashMap<ProjectContext.Key<*>, ProjectContext.Element>()

  override operator fun <E : Element> get(key: Key<E>): E {
    @Suppress("UNCHECKED_CAST")
    return cache.getOrPut(key) { key.invoke(project) } as E
  }
}
