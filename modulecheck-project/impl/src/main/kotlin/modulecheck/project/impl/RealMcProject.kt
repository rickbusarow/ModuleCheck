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

import modulecheck.api.context.resolvedDeclarationNames
import modulecheck.parsing.gradle.Configurations
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.gradle.SourceSets
import modulecheck.parsing.psi.asDeclarationName
import modulecheck.project.ExternalDependencies
import modulecheck.project.Logger
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.project.ProjectContext
import modulecheck.project.ProjectDependencies
import modulecheck.project.temp.AnvilGradlePlugin
import org.jetbrains.kotlin.name.FqName
import java.io.File

@Suppress("LongParameterList")
class RealMcProject(
  override val path: String,
  override val projectDir: File,
  override val buildFile: File,
  override val configurations: Configurations,
  override val hasKapt: Boolean,
  override val sourceSets: SourceSets,
  override val projectCache: ProjectCache,
  override val anvilGradlePlugin: AnvilGradlePlugin?,
  override val logger: Logger,
  projectDependencies: Lazy<ProjectDependencies>,
  externalDependencies: Lazy<ExternalDependencies>
) : McProject {

  override val projectDependencies: ProjectDependencies by projectDependencies

  override val externalDependencies: ExternalDependencies by externalDependencies

  private val context = ProjectContext(this)

  override fun clearContext() {
    context.clearContext()
  }

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

  override suspend fun resolveFqNameOrNull(
    declarationName: FqName,
    sourceSetName: SourceSetName
  ): FqName? {
    return resolvedDeclarationNames().getSource(
      declarationName.asDeclarationName(),
      sourceSetName
    )
      ?.run { declarationName }
  }
}
