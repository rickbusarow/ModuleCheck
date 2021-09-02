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

package modulecheck.api

import modulecheck.api.anvil.AnvilGradlePlugin
import modulecheck.api.context.ProjectContext
import modulecheck.api.context.ProjectContextImpl
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class AndroidProject2Impl(
  override val path: String,
  override val projectDir: File,
  override val buildFile: File,
  override val configurations: Map<ConfigurationName, Config>,
  override val projectDependencies: Lazy<ProjectDependencies>,
  override val hasKapt: Boolean,
  override val sourceSets: Map<SourceSetName, SourceSet>,
  override val projectCache: ConcurrentHashMap<String, Project2>,
  override val anvilGradlePlugin: AnvilGradlePlugin?,
  override val androidResourcesEnabled: Boolean,
  override val viewBindingEnabled: Boolean,
  override val resourceFiles: Set<File>,
  override val androidPackageOrNull: String?
) : AndroidProject2 {

  private val context = ProjectContextImpl(this)

  override fun <E : ProjectContext.Element> get(key: ProjectContext.Key<E>): E {
    return context[key]
  }

  override fun compareTo(other: Project2): Int = path.compareTo(other.path)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AndroidProject2Impl) return false

    if (path != other.path) return false

    return true
  }

  override fun hashCode(): Int {
    return path.hashCode()
  }

  override fun toString(): String {
    return "AndroidProject2Impl(path='$path')"
  }
}
