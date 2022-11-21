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

package modulecheck.project.generation

import modulecheck.model.dependency.ConfigFactory
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.ExternalDependencies
import modulecheck.model.dependency.McConfiguration
import modulecheck.model.dependency.ProjectDependencies
import modulecheck.model.dependency.asConfigurationName
import modulecheck.model.dependency.javaConfigurationNames
import modulecheck.model.sourceset.SourceSetName

data class ConfigBuilder(
  val name: ConfigurationName,
  val upstream: MutableList<ConfigurationName>,
  val downstream: MutableList<ConfigurationName>
) {
  fun toConfig(configFactory: ConfigFactory<String>): McConfiguration = configFactory.create(
    name.value
  )

  companion object {
    fun fromConfig(configuration: McConfiguration): ConfigBuilder {
      return ConfigBuilder(
        configuration.name,
        configuration.upstream.mapTo(mutableListOf()) { it.name },
        configuration.downstream.mapTo(mutableListOf()) { it.name }
      )
    }
  }
}

internal fun PlatformPluginBuilder<*>.configFactory(
  projectDependencies: ProjectDependencies,
  externalDependencies: ExternalDependencies
) = ConfigFactory(
  { this },
  { projectDependencies[this.asConfigurationName()].orEmpty() },
  { externalDependencies[this.asConfigurationName()].orEmpty() },
  { configurations.values.asSequence().map { it.name.value } },
  {
    configurations[asConfigurationName()]
      ?.upstream
      ?.map { it.value }
      .orEmpty()
  }
)

@PublishedApi
internal fun PlatformPluginBuilder<*>.populateConfigsFromSourceSets() {
  sourceSets
    .keys
    // add main source set configs first so that they can be safely looked up for inheriting configs
    .sortedByDescending { it == SourceSetName.MAIN }
    .flatMap { it.javaConfigurationNames() }
    .forEach { configurationName ->

      val upstream = if (configurationName.toSourceSetName() == SourceSetName.MAIN) {
        mutableListOf()
      } else {
        SourceSetName.MAIN.javaConfigurationNames()
          .map { configurations.getValue(it).name }
          .toMutableList()
      }

      val downstream = when {
        configurationName.toSourceSetName() != SourceSetName.MAIN -> emptyList()
        configurationName.isImplementation() -> listOf(configurationName.apiVariant())
        !configurationName.isApi() -> emptyList()
        else ->
          sourceSets.keys
            .filter { it != SourceSetName.MAIN }
            .flatMap { it.javaConfigurationNames() }
      }

      // val downstream = downstreamNames.map { configurations.getValue(it).name }

      configurations.putIfAbsent(
        configurationName,
        ConfigBuilder(
          name = configurationName,
          upstream.toMutableList(),
          downstream.toMutableList()
        )
      )
    }
}
