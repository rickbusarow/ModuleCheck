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

package modulecheck.project

import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.utils.mapToSet

sealed interface ConfiguredDependency : Dependency {
  val configurationName: ConfigurationName
  val name: String
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
  val accessor: String
) : Dependency

/**
 * [https://docs.gradle.org/current/userguide/plugins.html#sec:binary_plugins]
 *
 * @param name A descriptive name for this plugin, such as 'Kotlin kapt' or 'Android Gradle Plugin'
 * @param qualifiedId The canonical ID for the plugin, like `org.jetbrains.kotlin.kapt`
 * @param legacyIdOrNull An older, "legacy" ID for the plugin like `kotlin-kapt`
 * @param precompiledAccessorOrNull A special accessor invoked like a property, with or without
 *   backticks, like `base`.
 * @param kotlinFunctionArgumentOrNull The `kotlin(...)` function used for official libraries in Kotlin DSL
 *   files, like `kotlin("kapt")`.
 */
data class PluginDefinition(
  val name: String,
  val qualifiedId: String,
  val legacyIdOrNull: String?,
  val precompiledAccessorOrNull: String?,
  val kotlinFunctionArgumentOrNull: String?
) {
  val accessors by lazy {

    buildList {
      add("id(\"$qualifiedId\")")
      add("id \"$qualifiedId\"")
      add("id '$qualifiedId'")

      if (legacyIdOrNull != null) {
        add("id(\"$legacyIdOrNull\")")
        add("id \"$legacyIdOrNull\"")
        add("id '$legacyIdOrNull'")
      }

      if (precompiledAccessorOrNull != null) {
        if (precompiledAccessorOrNull.contains("-")) {
          add("`$precompiledAccessorOrNull`")
        } else {
          add(precompiledAccessorOrNull)
        }
      }
      if (kotlinFunctionArgumentOrNull != null) {
        add("kotlin(\"$kotlinFunctionArgumentOrNull\")")
      }
    }
      .mapToSet { PluginDependency(it) }
  }
}
