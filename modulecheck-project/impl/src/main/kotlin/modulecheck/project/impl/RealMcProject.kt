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

package modulecheck.project.impl

import modulecheck.api.context.resolvedDeclaredNames
import modulecheck.parsing.gradle.dsl.BuildFileParser
import modulecheck.parsing.gradle.model.PlatformPlugin
import modulecheck.parsing.gradle.model.ProjectPath.StringProjectPath
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.source.AnvilGradlePlugin
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.JavaVersion
import modulecheck.project.ExternalDependencies
import modulecheck.project.JvmFileProvider
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.project.ProjectContext
import modulecheck.project.ProjectDependencies
import modulecheck.reporting.logging.McLogger
import java.io.File

@Suppress("LongParameterList")
class RealMcProject(
  override val path: StringProjectPath,
  override val projectDir: File,
  override val buildFile: File,
  override val hasKapt: Boolean,
  override val hasTestFixturesPlugin: Boolean,
  override val projectCache: ProjectCache,
  override val anvilGradlePlugin: AnvilGradlePlugin?,
  override val logger: McLogger,
  override val jvmFileProviderFactory: JvmFileProvider.Factory,
  override val javaSourceVersion: JavaVersion,
  projectDependencies: Lazy<ProjectDependencies>,
  externalDependencies: Lazy<ExternalDependencies>,
  buildFileParserFactory: BuildFileParser.Factory,
  override val platformPlugin: PlatformPlugin
) : McProject {

  override val projectDependencies: ProjectDependencies by projectDependencies

  override val externalDependencies: ExternalDependencies by externalDependencies

  override val buildFileParser: BuildFileParser by lazy { buildFileParserFactory.create(this) }

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
    if (other !is McProject) return false

    if (path != other.path) return false

    return true
  }

  override fun hashCode(): Int {
    return path.hashCode()
  }

  override fun toString(): String {
    return "${this::class.java.simpleName}('$path')"
  }

  override suspend fun resolveFqNameOrNull(
    declaredName: DeclaredName,
    sourceSetName: SourceSetName
  ): DeclaredName? {
    return resolvedDeclaredNames().getSource(
      declaredName,
      sourceSetName
    )
      ?.run { declaredName }
  }
}
