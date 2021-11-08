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

import java.io.File
import kotlin.contracts.contract

@Suppress("TooManyFunctions")
interface McProject :
  ProjectContext,
  Comparable<McProject>,
  ProjectsAware {

  val path: String

  val projectDir: File
  val buildFile: File

  val configurations: Map<ConfigurationName, Config>

  val projectDependencies: ProjectDependencies

  val hasKapt: Boolean

  val sourceSets: Map<SourceSetName, SourceSet>
  val anvilGradlePlugin: AnvilGradlePlugin?
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

fun McProject.requireSourceOf(
  dependencyProject: McProject,
  sourceSetName: SourceSetName,
  isTestFixture: Boolean,
  apiOnly: Boolean
): ConfiguredProjectDependency {
  return sourceOfOrNull(
    dependencyProject = dependencyProject,
    sourceSetName = sourceSetName,
    isTestFixture = isTestFixture,
    apiOnly = apiOnly
  )
    ?: throw IllegalArgumentException(
      "Unable to find source of the dependency project '${dependencyProject.path}' in the " +
        "dependent project '$path', including transitive dependencies."
    )
}

fun McProject.sourceOfOrNull(
  dependencyProject: McProject,
  sourceSetName: SourceSetName,
  isTestFixture: Boolean,
  apiOnly: Boolean
): ConfiguredProjectDependency? {

  val baseConfigNames = if (apiOnly) {
    configurations[sourceSetName.apiConfig()]?.inherited
      .orEmpty()
      .map { it.name } + sourceSetName.apiConfig()
  } else {
    sourceSetName.configurationNames()
  }

  val testFixturesConfigNames = if (isTestFixture && apiOnly) {
    listOf(SourceSetName.TEST_FIXTURES.apiConfig())
  } else if (isTestFixture) {
    SourceSetName.TEST_FIXTURES.configurationNames()
  } else listOf()

  val toCheck = (baseConfigNames + testFixturesConfigNames)
    .mapNotNull { projectDependencies[it] }
    .flatten()

  return toCheck.firstOrNull { it.project == dependencyProject }
    ?: toCheck.firstOrNull { cpd ->
      cpd.project
        .sourceOfOrNull(
          dependencyProject = dependencyProject,
          sourceSetName = SourceSetName.MAIN,
          isTestFixture = cpd.isTestFixture,
          apiOnly = true
        ) != null
    }
}

interface AndroidMcProject : McProject {
  val androidResourcesEnabled: Boolean
  val viewBindingEnabled: Boolean
  val androidPackageOrNull: String?
  val manifests: Map<SourceSetName, File>
}
