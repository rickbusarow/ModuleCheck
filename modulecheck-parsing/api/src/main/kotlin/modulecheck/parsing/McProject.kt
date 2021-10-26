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

  val projectDependencies: Lazy<ProjectDependencies>

  val hasKapt: Boolean

  val sourceSets: Map<SourceSetName, SourceSet>
  val anvilGradlePlugin: AnvilGradlePlugin?
}

fun McProject.isAndroid(): Boolean {
  contract {
    returns(true) implies (this@isAndroid is AndroidMcProject)
  }
  return this is AndroidMcProject
}

fun McProject.sourceOf(
  dependencyProject: ConfiguredProjectDependency,
  apiOnly: Boolean = false
): ConfiguredProjectDependency? {
  val toCheck = if (apiOnly) {
    projectDependencies
      .value[ConfigurationName.api]
      .orEmpty()
  } else {
    projectDependencies
      .value
      .main()
  }

  if (dependencyProject in toCheck) return ConfiguredProjectDependency(
    configurationName = dependencyProject.configurationName,
    project = this,
    isTestFixture = dependencyProject.isTestFixture
  )

  return toCheck.firstOrNull {
    it == dependencyProject || it.project.sourceOf(dependencyProject, true) != null
  }
}

interface AndroidMcProject : McProject {
  val androidResourcesEnabled: Boolean
  val viewBindingEnabled: Boolean
  val resourceFiles: Set<File>
  val androidPackageOrNull: String?
  val manifests: Map<SourceSetName, File>
}
