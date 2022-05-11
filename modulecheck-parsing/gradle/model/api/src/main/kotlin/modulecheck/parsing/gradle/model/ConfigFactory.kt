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

package modulecheck.parsing.gradle.model

import modulecheck.utils.decapitalize
import modulecheck.utils.mapToSet

class ConfigFactory<T : Any>(
  private val identifier: T.() -> String,
  private val allFactory: () -> Sequence<T>,
  private val extendsFrom: String.() -> List<T>
) {
  fun create(t: T): Config {

    return Config(
      name = t.identifier().asConfigurationName(),
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
        .flatMap {

          if (it.identifier().startsWith("testFixtures")) {
            val withoutPrefix = it.identifier().removePrefix("testFixtures").decapitalize()

            withoutPrefix.extendsFrom()
              .plus(it.identifier().extendsFrom())
          } else {
            it.identifier().extendsFrom()
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
