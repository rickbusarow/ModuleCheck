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

import modulecheck.parsing.gradle.Config
import modulecheck.parsing.gradle.asConfigurationName
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer

fun Configuration.withUpstream(): Sequence<Configuration> {

  return generateSequence(sequenceOf(this)) { configurations ->
    configurations.flatMap { it.extendsFrom }
      .takeIf<Sequence<Configuration>> { it.iterator().hasNext() }
  }
    .flatten()
    .distinct()
}

fun Configuration.withDownstream(
  configurationContainer: ConfigurationContainer
): Sequence<Configuration> {
  return generateSequence(sequenceOf(this)) { configurations ->
    configurations.flatMap { config ->
      configurationContainer
        .asSequence()
        .filter { it.extendsFrom.contains(config) }
    }
      .takeIf<Sequence<Configuration>> { it.iterator().hasNext() }
  }
    .flatten()
    .distinct()
}

fun Configuration.toConfig(
  configurationContainer: ConfigurationContainer
): Config {

  return Config(
    name = name.asConfigurationName(),
    upstreamSequence = withUpstream()
      .drop(1)
      .map { it.toConfig(configurationContainer) },
    downstreamSequence = withDownstream(configurationContainer)
      .drop(1)
      .map { it.toConfig(configurationContainer) }
  )
}
