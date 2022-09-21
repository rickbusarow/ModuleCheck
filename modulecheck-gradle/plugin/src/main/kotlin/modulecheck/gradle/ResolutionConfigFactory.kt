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

    val configuration = configurations.first()

    val copy = configuration.copyRecursive().setTransitive(false)
    copy.isCanBeResolved = true

    // Resolve using the latest version of explicitly declared dependencies and retains Kotlin's
    // inherited stdlib dependencies from the super configurations. This is required for variant
    // resolution, but the full set can break consumer capability matching.
    val inherited = configuration.allDependencies
      .filterIsInstance<ExternalDependency>()
      .filter { dependency -> dependency.group == "org.jetbrains.kotlin" }
      .filter { dependency -> dependency.version != null } -
      configuration.dependencies

    val latest = configuration.dependencies
      .filter { dependency -> dependency !is ProjectDependency }

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

    copy.dependencies.clear()
    copy.dependencies.addAll(latest)
    copy.dependencies.addAll(inherited)

    copy.addAttributes(configuration)
    return copy
  }

  private fun HasConfigurableAttributes<*>.addAttributes(
    source: HasConfigurableAttributes<*>,
    filter: (String) -> Boolean = { true }
  ) {
    attributes { container ->
      for (key in source.attributes.keySet()) {
        if (filter.invoke(key.name)) {
          val value = source.attributes.getAttribute(key as Attribute<Any>)
          container.attribute(key, value)
        }
      }
    }
  }
}
