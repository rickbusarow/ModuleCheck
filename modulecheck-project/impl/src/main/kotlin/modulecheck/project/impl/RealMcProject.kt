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
import modulecheck.model.dependency.ExternalDependencies
import modulecheck.model.dependency.PlatformPlugin
import modulecheck.model.dependency.ProjectDependencies
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.gradle.dsl.BuildFileParser
import modulecheck.parsing.source.AnvilGradlePlugin
import modulecheck.parsing.source.QualifiedDeclaredName
import modulecheck.project.JvmFileProvider.Factory
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.project.ProjectContext
import modulecheck.reporting.logging.McLogger
import org.jetbrains.kotlin.config.JvmTarget
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
  override val jvmFileProviderFactory: Factory,
  override val jvmTarget: JvmTarget,
  buildFileParserFactory: BuildFileParser.Factory,
  override val platformPlugin: PlatformPlugin
) : McProject {

  override val projectDependencies: ProjectDependencies by lazy {
    ProjectDependencies(platformPlugin.configurations.mapValues { it.value.projectDependencies })
  }
  override val externalDependencies: ExternalDependencies by lazy {
    ExternalDependencies(platformPlugin.configurations.mapValues { it.value.externalDependencies })
  }

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
    declaredName: QualifiedDeclaredName,
    sourceSetName: SourceSetName
  ): QualifiedDeclaredName? {
    return resolvedDeclaredNames().getSource(
      declaredName,
      sourceSetName
    )
      ?.run { declaredName }
  }
}
