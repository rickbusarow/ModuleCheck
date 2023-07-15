/*
 * Copyright (C) 2021-2023 Rick Busarow
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

package modulecheck.gradle

import modulecheck.gradle.platforms.internal.GradleConfiguration
import modulecheck.gradle.platforms.internal.GradleProject
import modulecheck.gradle.platforms.internal.GradleProjectDependency
import modulecheck.model.dependency.apiConfig
import modulecheck.model.dependency.asConfigurationName
import modulecheck.model.sourceset.SourceSetName
import modulecheck.utils.filterToSet
import modulecheck.utils.flatMapToSet
import modulecheck.utils.singletonSet
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.SelfResolvingDependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.HasConfigurableAttributes
import org.gradle.api.internal.artifacts.DefaultDependencySet
import org.gradle.api.internal.artifacts.configurations.ConfigurationInternal.InternalState.UNRESOLVED
import org.gradle.api.internal.artifacts.configurations.DefaultConfiguration

/** A factory class for creating resolution configurations. */
class ResolutionConfigFactory {

  /**
   * Fetches the external dependencies of a project dependency.
   *
   * @param sourceConfiguration The configuration from which the dependencies are fetched.
   * @param visited A set of visited configurations to avoid cyclic dependencies.
   * @return A set of dependencies.
   */
  private fun GradleProjectDependency.externalDependencies(
    sourceConfiguration: GradleConfiguration,
    visited: Set<String>
  ): Set<Dependency> {

    val contributingConfigName = sourceConfiguration.name
      .asConfigurationName()
      .toSourceSetName()
      .apiConfig()

    val contributingConfig = dependencyProject.configurations
      .findByName(contributingConfigName.value)
      ?.takeIf { it.name.asConfigurationName().toSourceSetName() != SourceSetName.TEST }
      ?: dependencyProject.configurations.findByName("api")
      ?: return emptySet()

    val deps = contributingConfig.dependencies.toSet()

    return deps.plus(
      deps.flatMapToSet { dep ->
        if (dep is GradleProjectDependency && contributingConfig.name !in visited) {

          dep.externalDependencies(
            contributingConfig,
            visited + contributingConfig.name
          )
        } else {
          emptySet()
        }
      }
    )
      .filterToSet { it !is GradleProjectDependency }
  }

  /**
   * Creates a new configuration for a given project and source configuration.
   *
   * @param project The project for which the configuration is created.
   * @param sourceConfiguration The source configuration.
   * @return The created configuration.
   */
  fun create(
    project: GradleProject,
    sourceConfiguration: GradleConfiguration
  ): GradleConfiguration {

    val copy = sourceConfiguration.copyRecursive().setTransitive(true)

    copy as DefaultConfiguration

    if (copy.resolvedState == UNRESOLVED) {
      copy.isCanBeResolved = true
    }

    copy.dependencies.clear()

    val dest = copy.dependencies as DefaultDependencySet

    sourceConfiguration.excludeRules.forEach { er ->
      copy.exclude(mapOf("group" to er.group, "module" to er.module))
    }

    if (copy.resolvedState == UNRESOLVED) {

      val deps = sourceConfiguration.allDependencies
        .flatMapToSet { dependency ->

          when (dependency) {
            is GradleProjectDependency -> dependency.externalDependencies(
              sourceConfiguration,
              setOf(sourceConfiguration.name)
            )

            else -> dependency.singletonSet()
          }
            .filterToSet { it !is GradleProjectDependency && it !is SelfResolvingDependency }
        }

      dest.addAll(deps)
    }

    val metadata = project.configurations.findByName("commonMainMetadataElements")
    if (metadata == null) {
      val compile = project.configurations.findByName("compile")
      if (compile != null) {
        copy.addAttributes(compile) { key -> key.contains("kotlin") }
      }
    } else {
      copy.addAttributes(metadata)
    }

    copy.addAttributes(sourceConfiguration)
    return copy
  }

  /**
   * Adds attributes from a source configuration to the current configuration.
   *
   * @param source The source configuration from which the attributes are fetched.
   * @param predicate A function to filter the attributes to be added.
   */
  private fun HasConfigurableAttributes<*>.addAttributes(
    source: HasConfigurableAttributes<*>,
    predicate: (String) -> Boolean = { true }
  ) {
    attributes { container ->
      source.attributes.keySet()
        .filter { predicate(it.name) }
        .forEach { key ->
          @Suppress("UNCHECKED_CAST")
          val value = source.attributes.getAttribute(key as Attribute<Any>)!!
          container.attribute(key, value)
        }
    }
  }
}
