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

package modulecheck.gradle

import modulecheck.model.dependency.apiConfig
import modulecheck.model.dependency.asConfigurationName
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.gradle.model.GradleProject
import modulecheck.utils.flatMapToSet
import modulecheck.utils.singletonSet
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.HasConfigurableAttributes
import org.gradle.api.internal.artifacts.DefaultDependencySet
import org.gradle.api.internal.artifacts.configurations.ConfigurationInternal.InternalState.UNRESOLVED
import org.gradle.api.internal.artifacts.configurations.DefaultConfiguration
import org.gradle.api.internal.artifacts.dependencies.ProjectDependencyInternal

class ResolutionConfigFactory {

  private fun ProjectDependency.externalDependencies(
    sourceConfiguration: Configuration,
    visited: Set<String>
  ): Set<Dependency> {

    // println("<Rick> 40 -- ${dependencyProject.path}  --  ${sourceConfiguration.name}")

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
        if (dep is ProjectDependencyInternal && contributingConfig.name !in visited) {
          dep.externalDependencies(contributingConfig, visited + contributingConfig.name)
        } else {
          emptySet()
        }
      }
    )
  }

  fun create(
    project: GradleProject,
    sourceConfiguration: Configuration
  ): Configuration {

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

    sourceConfiguration.allDependencies
      .configureEach { dependency ->

        if (copy.resolvedState == UNRESOLVED) {

          val deps = when (dependency) {
            is ProjectDependency -> dependency.externalDependencies(
              sourceConfiguration,
              setOf(sourceConfiguration.name)
            )
              .filter { it !is ProjectDependency }

            else -> dependency.singletonSet()
          }

          for (trans in deps) {
            dest.add(trans)
          }
        }
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
