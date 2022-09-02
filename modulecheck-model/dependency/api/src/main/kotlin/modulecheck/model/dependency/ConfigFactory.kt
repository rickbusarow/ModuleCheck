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

import modulecheck.utils.decapitalize
import modulecheck.utils.mapToSet

class ConfigFactory<T : Any>(
  private val identifier: T.() -> String,
  private val projectDependencies: T.() -> Set<ProjectDependency>,
  private val externalDependencies: T.() -> Set<ExternalDependency>,
  private val allFactory: () -> Sequence<T>,
  private val extendsFrom: String.() -> List<T>
) {
  fun create(t: T): McConfiguration {

    return McConfiguration(
      name = t.identifier().asConfigurationName(),
      projectDependencies = projectDependencies(t),
      externalDependencies = externalDependencies(t),
      upstreamSequence = t.withUpstream()
        .drop(1)
        .map { create(it) },
      downstreamSequence = t.withDownstream()
        .drop(1)
        .map { create(it) }
    )
  }

  private fun T.withUpstream(): Sequence<T> {

    return generateSequence(sequenceOf(this)) { configurations ->
      configurations
        .flatMap { t ->

          val identifier = t.identifier()

          if (identifier.startsWith("testFixtures")) {
            identifier.removePrefix("testFixtures")
              .decapitalize()
              .extendsFrom()
              .plus(identifier.extendsFrom())
          } else {
            identifier.extendsFrom()
          }
        }
        .takeIf { it.iterator().hasNext() }
    }
      .flatten()
      .distinct()
  }

  private fun T.withDownstream(): Sequence<T> {
    return generateSequence(sequenceOf(this)) { configurations ->
      configurations
        .flatMap { config ->
          allFactory()
            .filter {
              it.identifier().extendsFrom()
                .mapToSet { it.identifier() }
                .contains(config.identifier())
            }
        }
        .takeIf { it.iterator().hasNext() }
    }
      .flatten()
      .distinct()
  }
}
