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

import modulecheck.parsing.gradle.model.GradleProject
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.HasConfigurableAttributes
import org.gradle.api.internal.artifacts.DefaultDependencySet
import org.gradle.api.internal.artifacts.configurations.ConfigurationInternal.InternalState.UNRESOLVED
import org.gradle.api.internal.artifacts.configurations.DefaultConfiguration

class ResolutionConfigFactory {

  fun create(
    project: GradleProject,
    configuration: Configuration
  ): Configuration {

    val copy = configuration.copyRecursive().setTransitive(true)

    copy.isCanBeResolved = true

    copy.dependencies.clear()

    val dest = copy.dependencies as DefaultDependencySet

    configuration.excludeRules.forEach { er ->
      copy.exclude(mapOf("group" to er.group, "module" to er.module))
    }

    copy as DefaultConfiguration

    copy.resolvedState

    configuration.allDependencies
      .matching { dependency -> dependency !is ProjectDependency }
      .configureEach {
        if (copy.resolvedState == UNRESOLVED) {
          dest.add(it)
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

    copy.addAttributes(configuration)
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
