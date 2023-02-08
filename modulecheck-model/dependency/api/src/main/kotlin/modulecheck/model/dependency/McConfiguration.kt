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

package modulecheck.model.dependency

/**
 * Cache of [configurations][McConfiguration], probably at the project level.
 *
 * @since 0.13.0
 */
class Configurations(
  delegate: Map<ConfigurationName, McConfiguration>
) : Map<ConfigurationName, McConfiguration> by delegate {

  override fun toString(): String {
    return toList().joinToString("\n")
  }
}

data class McConfiguration(
  val name: ConfigurationName,
  val projectDependencies: Set<ProjectDependency>,
  val externalDependencies: Set<ExternalDependency>,
  private val upstreamSequence: Sequence<McConfiguration>,
  private val downstreamSequence: Sequence<McConfiguration>
) {

  /**
   * @return list of all other configurations which this configuration depends upon. The list is
   *   breadth-first, with the most-downstream configurations being last.
   * @since 0.13.0
   */
  val upstream: List<McConfiguration> by lazy { upstreamSequence.toList() }

  /**
   * @return list of all other configurations which depend upon this configuration. The list is
   *   breadth-first, with the most-downstream configurations being last.
   * @since 0.13.0
   */
  val downstream: List<McConfiguration> by lazy { downstreamSequence.toList() }

  /**
   * @return list of this configuration and all other configurations which it depends upon. The list
   *   is breadth-first, with the most-upstream configurations being last.
   * @since 0.13.0
   */
  fun withUpstream(): List<McConfiguration> = listOf(this) + upstream

  /**
   * @return list of this configuration and all other configurations which depend upon it. The list
   *   is breadth-first, with the most-downstream configurations being last.
   * @since 0.13.0
   */
  fun withDownstream(): List<McConfiguration> = listOf(this) + downstream

  override fun toString(): String {
    return """Config   --  name=${name.value}
    |  upstream=${upstream.map { it.name.value }}
    |  downstream=${downstream.map { it.name.value }}
    |)
    """.trimMargin()
  }
}

/**
 * convenience for `map { it.name }`
 *
 * @since 0.13.0
 */
fun Iterable<McConfiguration>.names(): List<ConfigurationName> = map { it.name }

/**
 * convenience for `map { it.name }`
 *
 * @since 0.13.0
 */
fun Sequence<McConfiguration>.names(): Sequence<ConfigurationName> = map { it.name }
