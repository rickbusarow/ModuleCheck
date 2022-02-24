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

import modulecheck.parsing.gradle.HasBuildFile
import modulecheck.parsing.gradle.HasConfigurations
import modulecheck.parsing.gradle.HasDependencyDeclarations
import modulecheck.parsing.gradle.HasPath
import modulecheck.parsing.gradle.InvokesConfigurationNames
import modulecheck.parsing.gradle.PluginAware
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.parsing.source.AnvilGradlePlugin
import modulecheck.parsing.source.JavaVersion
import org.jetbrains.kotlin.name.FqName
import java.io.File
import kotlin.contracts.contract

@Suppress("TooManyFunctions")
interface McProject :
  ProjectContext,
  Comparable<McProject>,
  HasPath,
  HasProjectCache,
  HasBuildFile,
  HasConfigurations,
  HasDependencyDeclarations,
  InvokesConfigurationNames,
  PluginAware {

  val projectDir: File

  val projectDependencies: ProjectDependencies
  val externalDependencies: ExternalDependencies

  val anvilGradlePlugin: AnvilGradlePlugin?

  override val hasAnvil: Boolean
    get() = anvilGradlePlugin != null

  override val hasAGP: Boolean
    get() = this is AndroidMcProject

  val buildFileParser: BuildFileParser

  val logger: Logger
  val jvmFileProviderFactory: JvmFileProvider.Factory

  val javaSourceVersion: JavaVersion

  override suspend fun getConfigurationInvocations(): Set<String> = configurationInvocations()

  suspend fun resolveFqNameOrNull(
    declarationName: FqName,
    sourceSetName: SourceSetName
  ): FqName?
}

private suspend fun McProject.configurationInvocations(): Set<String> {
  return buildFileParser.dependenciesBlocks()
    .flatMap { it.settings }
    .mapNotNull { declaration ->

      val declarationText = declaration.declarationText.trim()

      declaration.configName.value
        .takeIf { declarationText.startsWith(it) }
    }
    .toSet()
}

fun McProject.isAndroid(): Boolean {
  contract {
    returns(true) implies (this@isAndroid is AndroidMcProject)
  }
  return this is AndroidMcProject
}

interface AndroidMcProject : McProject {
  val androidResourcesEnabled: Boolean
  val viewBindingEnabled: Boolean
  val manifests: Map<SourceSetName, File>
}
