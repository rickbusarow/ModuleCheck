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

package modulecheck.project.impl

import modulecheck.project.Config
import modulecheck.project.ConfigurationName
import modulecheck.project.ExternalDependencies
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.project.ProjectContext
import modulecheck.project.ProjectDependencies
import modulecheck.project.RealProjectContext
import modulecheck.project.SourceSet
import modulecheck.project.SourceSetName
import modulecheck.project.temp.AnvilGradlePlugin
import java.io.File

@Suppress("LongParameterList")
class RealMcProject(
  override val path: String,
  override val projectDir: File,
  override val buildFile: File,
  override val configurations: Map<ConfigurationName, Config>,
  override val hasKapt: Boolean,
  override val sourceSets: Map<SourceSetName, SourceSet>,
  override val projectCache: ProjectCache,
  override val anvilGradlePlugin: AnvilGradlePlugin?,
  projectDependencies: Lazy<ProjectDependencies>,
  externalDependencies: Lazy<ExternalDependencies>
) : McProject {

  override val projectDependencies: ProjectDependencies by projectDependencies

  override val externalDependencies: ExternalDependencies by externalDependencies

  private val context = RealProjectContext(this)

  override suspend fun <E : ProjectContext.Element> get(key: ProjectContext.Key<E>): E {
    return context.get(key)
  }

  override fun compareTo(other: McProject): Int = path.compareTo(other.path)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is RealMcProject) return false

    if (path != other.path) return false

    return true
  }

  override fun hashCode(): Int {
    return path.hashCode()
  }

  override fun toString(): String {
    return "McProject('$path')"
  }
}
