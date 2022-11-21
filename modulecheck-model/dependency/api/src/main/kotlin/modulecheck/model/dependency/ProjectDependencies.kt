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

package modulecheck.model.dependency

import modulecheck.model.sourceset.SourceSetName
import modulecheck.utils.filterToSet
import modulecheck.utils.flatMapToSet

class ProjectDependencies(
  map: Map<ConfigurationName, Set<ProjectDependency>>
) : MutableMap<ConfigurationName, Set<ProjectDependency>> by map.toMutableMap() {
  fun main(): Set<ProjectDependency> = ConfigurationName.main()
    .flatMapToSet { configurationName ->
      get(configurationName).orEmpty()
    }

  fun public(): Set<ProjectDependency> = ConfigurationName.public()
    .flatMapToSet { configurationName ->
      get(configurationName).orEmpty()
    }

  fun private(): Set<ProjectDependency> = ConfigurationName.private()
    .flatMapToSet { configurationName ->
      get(configurationName).orEmpty()
    }

  operator fun get(sourceSetName: SourceSetName): Set<ProjectDependency> {
    return sourceSetName.javaConfigurationNames().flatMapToSet { get(it).orEmpty() }
  }

  fun add(cpd: ProjectDependency) {
    val oldDependencies = get(cpd.configurationName).orEmpty()
    put(cpd.configurationName, oldDependencies + cpd)
  }

  fun remove(cpd: ProjectDependency) {
    val oldDependencies = get(cpd.configurationName).orEmpty()
    put(cpd.configurationName, oldDependencies.filterToSet { it != cpd })
  }
}
