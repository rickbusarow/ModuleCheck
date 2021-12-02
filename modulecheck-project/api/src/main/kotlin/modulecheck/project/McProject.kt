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

package modulecheck.project

import modulecheck.dagger.AppScope
import modulecheck.dagger.SingleIn
import modulecheck.project.temp.AnvilGradlePlugin
import org.jetbrains.kotlin.name.FqName
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.inject.Inject
import kotlin.contracts.contract

@Suppress("TooManyFunctions")
interface McProject :
  ProjectContext,
  Comparable<McProject>,
  HasProjectCache {

  val path: String

  val projectDir: File
  val buildFile: File

  val configurations: Configurations

  val projectDependencies: ProjectDependencies
  val externalDependencies: ExternalDependencies

  val hasKapt: Boolean

  val sourceSets: SourceSets
  val anvilGradlePlugin: AnvilGradlePlugin?

  val logger: Logger

  suspend fun resolveFqNameOrNull(
    declarationName: FqName,
    sourceSetName: SourceSetName
  ): FqName?
}

/**
 * Reverse lookup of all the configurations which inherit another configuration.
 *
 * For instance, every java/kotlin configuration (`implementation`, `testImplementation`, etc.)
 * within a project inherits from the common `api` configuration,
 * so `someProject.inheritingConfigurations(ConfigurationName.api)` would return all other
 * java/kotlin configurations within that project.
 */
fun McProject.inheritingConfigurations(configurationName: ConfigurationName): Set<Config> {
  return configurations.values
    .filter { inheritingConfig ->
      inheritingConfig.inherited
        .any { inheritedConfig ->
          inheritedConfig.name == configurationName
        }
    }.toSet()
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
  val androidPackageOrNull: String?
  val androidRFqNameOrNull: String?
  val manifests: Map<SourceSetName, File>
}

@SingleIn(AppScope::class)
class ProjectCache @Inject constructor() : ConcurrentMap<String, McProject> by ConcurrentHashMap()
