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

import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.parsing.gradle.model.Identifier
import modulecheck.parsing.gradle.model.PluginAccessor

sealed interface ConfiguredDependency : Dependency {

  val configurationName: ConfigurationName

  /**
   * The path/id/coordinates of the dependency.
   *
   * For a [ProjectDependency], this is a Gradle path like `:common:ui:widgets`.
   *
   * For an [ExternalDependency], this is the Maven coordinates with or without a version, like
   * `com.example.foo:ui-widgets:1.0.0`.
   */
  val identifier: Identifier

  /** Is the dependency being invoked via `testFixtures(project(...))`? */
  val isTestFixture: Boolean

  companion object {

    inline fun <reified T : ConfiguredDependency> T.copy(
      configurationName: ConfigurationName = this.configurationName,
      isTestFixture: Boolean = this.isTestFixture
    ): ConfiguredDependency = when (this as ConfiguredDependency) {
      is ExternalDependency -> (this as ExternalDependency).copy(
        configurationName = configurationName,
        group = group,
        moduleName = moduleName,
        version = version,
        isTestFixture = isTestFixture
      )

      is ProjectDependency -> (this as ProjectDependency).copy(
        configurationName = configurationName,
        path = path,
        isTestFixture = isTestFixture
      )
    }
  }
}

sealed interface Dependency

/**
 * [https://docs.gradle.org/current/userguide/plugins.html#sec:binary_plugins]
 *
 * @param accessor Could be any of:
 *    - standard `id` invocations
 *       - `id 'org.jetbrains.kotlin.kapt'` (groovy) or `id("org.jetbrains.kotlin.kapt")` (kotlin)
 *       - `id 'kotlin-kapt'` (groovy) or `id("kotlin-kapt")` (kotlin)
 *    - precompiled accessor for Gradle plugins or `buildSrc`
 *       - `java`, `maven-publish`, `my-convention-plugin`
 *    - function invocations for Kotlin libraries in the Kotlin DSL only
 *       - `kotlin("kapt")`
 *    - alias invocations for Gradle's type-safe catalogs
 *       - `alias(libs.plugins.anvil)`
 */
data class PluginDependency(
  val accessor: PluginAccessor
) : Dependency {
  companion object {
    /** @return a [PluginDependency] wrapping the [PluginAccessor] receiver */
    fun PluginAccessor.toPluginDependency(): PluginDependency = PluginDependency(this)
  }
}
