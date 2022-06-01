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

import modulecheck.model.dependency.ProjectDependency
import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.parsing.gradle.model.SourceSetName

class ProjectDependencies(
  map: MutableMap<ConfigurationName, List<ProjectDependency>>
) : MutableMap<ConfigurationName, List<ProjectDependency>> by map {
  fun main(): List<ProjectDependency> = ConfigurationName.main()
    .flatMap { configurationName ->
      get(configurationName).orEmpty()
    }

  fun public(): List<ProjectDependency> = ConfigurationName.public()
    .flatMap { configurationName ->
      get(configurationName).orEmpty()
    }

  fun private(): List<ProjectDependency> = ConfigurationName.private()
    .flatMap { configurationName ->
      get(configurationName).orEmpty()
    }

  operator fun get(sourceSetName: SourceSetName): List<ProjectDependency> {
    return sourceSetName.javaConfigurationNames().flatMap { get(it).orEmpty() }
  }

  fun add(cpd: ProjectDependency) {
    val oldDependencies = get(cpd.configurationName) ?: emptyList()
    put(cpd.configurationName, oldDependencies + cpd)
  }

  fun remove(cpd: ProjectDependency) {
    val oldDependencies = get(cpd.configurationName) ?: emptyList()
    put(cpd.configurationName, oldDependencies.filter { it != cpd })
  }
}
