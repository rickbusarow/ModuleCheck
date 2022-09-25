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
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.HasConfigurableAttributes
import org.gradle.api.internal.artifacts.DefaultDependencySet
import org.gradle.api.internal.artifacts.DefaultExcludeRule
import org.gradle.api.internal.artifacts.configurations.ConfigurationInternal.InternalState.UNRESOLVED
import org.gradle.api.internal.artifacts.configurations.DefaultConfiguration

/**
 * Adapted from Ben Manes' gradle versions plugin
 * https://github.com/ben-manes/gradle-versions-plugin/blob/0310db508ced15b5d91a36541823ecfb3ddcc735/gradle-versions-plugin/src/main/kotlin/com/github/benmanes/gradle/versions/updates/Resolver.kt.
 *
 * @since 0.13.0
 */
class ResolutionConfigFactory {

  fun create(
    project: GradleProject,
    configurations: List<Configuration>
  ): Configuration {

    val first = configurations.first()

    val copy = first.copyRecursive().setTransitive(false)

    copy.isCanBeResolved = true

    val dest = copy.dependencies as DefaultDependencySet

    copy.dependencies.clear()

    println("   -------   ${project.path} ")

    configurations.forEach { configuration ->

      configuration.excludeRules.forEach { er ->
        copy.excludeRules.add(DefaultExcludeRule(er.group, er.module))
      }

      copy as DefaultConfiguration

      copy.resolvedState

      // Resolve using the latest version of explicitly declared dependencies and retains Kotlin's
      // inherited stdlib dependencies from the super configurations. This is required for variant
      // resolution, but the full set can break consumer capability matching.
      configuration.allDependencies
        .matching { dependency -> dependency !in configuration.dependencies }
        .matching { dependency -> dependency is ExternalDependency }
        .matching { dependency -> dependency.group == "org.jetbrains.kotlin" }
        .matching { dependency -> dependency.version != null }
        .configureEach {
          if (copy.resolvedState == UNRESOLVED) {
            dest.add(it)
          }
        }

      configuration.allDependencies
        .matching { dependency -> dependency !is ProjectDependency }
        .configureEach {
          if (copy.resolvedState == UNRESOLVED) {
            dest.add(it)
          }
        }
    }

    // Adds the Kotlin 1.2.x legacy metadata to assist in variant selection
    val metadata = project.configurations.findByName("commonMainMetadataElements")
    if (metadata == null) {
      val compile = project.configurations.findByName("compile")
      if (compile != null) {
        copy.addAttributes(compile) { key -> key.contains("kotlin") }
      }
    } else {
      copy.addAttributes(metadata)
    }

    copy.addAttributes(first)
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
