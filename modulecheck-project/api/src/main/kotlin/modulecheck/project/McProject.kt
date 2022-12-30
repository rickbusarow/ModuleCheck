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

import modulecheck.model.dependency.Configurations
import modulecheck.model.dependency.ExternalDependencies
import modulecheck.model.dependency.HasConfigurations
import modulecheck.model.dependency.HasDependencies
import modulecheck.model.dependency.HasPath
import modulecheck.model.dependency.HasSourceSets
import modulecheck.model.dependency.ProjectDependencies
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.model.dependency.SourceSets
import modulecheck.model.dependency.isAndroid
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.gradle.dsl.HasBuildFile
import modulecheck.parsing.gradle.dsl.HasDependencyDeclarations
import modulecheck.parsing.gradle.dsl.InvokesConfigurationNames
import modulecheck.parsing.gradle.model.HasPlatformPlugin
import modulecheck.parsing.gradle.model.PluginAware
import modulecheck.parsing.source.AnvilGradlePlugin
import modulecheck.parsing.source.QualifiedDeclaredName
import modulecheck.parsing.source.ResolvableMcName
import modulecheck.reporting.logging.McLogger
import org.jetbrains.kotlin.config.JvmTarget
import java.io.File

@Suppress("TooManyFunctions")
interface McProject :
  ProjectContext,
  Comparable<McProject>,
  HasPath,
  HasProjectCache,
  HasBuildFile,
  HasConfigurations,
  HasDependencies,
  HasSourceSets,
  HasDependencyDeclarations,
  InvokesConfigurationNames,
  HasPlatformPlugin,
  PluginAware {

  override val path: StringProjectPath

  override val configurations: Configurations
    get() = platformPlugin.configurations

  override val sourceSets: SourceSets
    get() = platformPlugin.sourceSets

  val projectDir: File

  override val projectDependencies: ProjectDependencies
  override val externalDependencies: ExternalDependencies

  val anvilGradlePlugin: AnvilGradlePlugin?

  override val hasAnvil: Boolean
    get() = anvilGradlePlugin != null
  override val hasAGP: Boolean
    get() = platformPlugin.isAndroid()

  val logger: McLogger
  val jvmFileProviderFactory: JvmFileProvider.Factory

  /**
   * The Java version used to compile this project
   *
   * @since 0.12.0
   */
  val jvmTarget: JvmTarget

  /**
   * @return a [QualifiedDeclaredName] if one can be found for the given [resolvableMcName] and
   *     [sourceSetName]
   * @since 0.12.0
   */
  suspend fun resolvedNameOrNull(
    resolvableMcName: ResolvableMcName,
    sourceSetName: SourceSetName
  ): QualifiedDeclaredName?
}

fun McProject.isAndroid(): Boolean = platformPlugin.isAndroid()
