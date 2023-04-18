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

import modulecheck.model.sourceset.SourceSetName
import modulecheck.model.sourceset.asSourceSetName
import modulecheck.utils.capitalize
import modulecheck.utils.decapitalize

/**
 * Wraps the unqualified, simple name of a Gradle
 * Configuration, like `implementation` or `debugApi`.
 *
 * @property value the name
 */
@JvmInline
value class ConfigurationName(val value: String) : Comparable<ConfigurationName> {

  /**
   * Strips the "base Configuration name" (`api`, `implementation`, `compileOnly`,
   * `runtimeOnly`) from an aggregate name like `debugImplementation`.
   *
   * examples:
   *
   * ```
   * Config                           SourceSet
   * | api                            | main
   * | compileOnlyApi                 | main
   * | implementation                 | main
   * | debugImplementation            | debug
   * | testImplementation             | test
   * | internalReleaseImplementation  | internalRelease
   * ```
   *
   * @return the name of the source set used with this configuration, wrapped in [SourceSetName]
   */
  fun toSourceSetName(): SourceSetName = when (this.value) {
    // "main" source set configurations omit the "main" from their name,
    // creating "implementation" instead of "mainImplementation"
    in mainConfigurations -> SourceSetName.MAIN
    // all other configurations (like "test", "debug", or "androidTest")
    // are just "$sourceSetName${baseConfigurationName.capitalize()}"
    else -> this.value.extractSourceSetName()
  }

  /**
   * Returns the base name of the Configuration without any source set prefix.
   *
   * For "main" source sets, this function just returns the same string,
   * e.g.: ConfigurationName("api").nameWithoutSourceSet() == "api"
   * ConfigurationName("implementation").nameWithoutSourceSet() == "implementation"
   *
   * For other source sets, it returns the base configuration names:
   * ConfigurationName("debugApi").nameWithoutSourceSet() == "Api"
   * ConfigurationName("testImplementation").nameWithoutSourceSet() == "Implementation"
   *
   * @since 0.12.0
   */
  fun nameWithoutSourceSet(): String {
    return when {
      isKapt() -> ConfigurationName.kapt.value
      else -> value.removePrefix(toSourceSetName().value)
    }
  }

  /**
   * Returns the base name of the Configuration without any source set prefix.
   *
   * For "main" source sets, this function just returns the same string,
   * e.g.: ConfigurationName("api").nameWithoutSourceSet() == "api"
   * ConfigurationName("implementation").nameWithoutSourceSet() == "implementation"
   *
   * For other source sets, it returns the base configuration names:
   * ConfigurationName("debugApi").nameWithoutSourceSet() == "Api"
   * ConfigurationName("testImplementation").nameWithoutSourceSet() == "Implementation"
   *
   * @since 0.12.0
   */
  fun switchSourceSet(newSourceSetName: SourceSetName): ConfigurationName {

    return when {
      isKapt() -> ConfigurationName(
        "${nameWithoutSourceSet()}${newSourceSetName.value.capitalize()}"
      )

      else -> ConfigurationName(
        "${newSourceSetName.value}${nameWithoutSourceSet().capitalize()}"
      )
    }
  }

  /**
   * find the "base" configuration name and remove it
   *
   * For instance, `debugCompileOnly` would find the "CompileOnly"
   * and remove it, returning "debug" as the sourceSet name
   *
   * @since 0.12.0
   */
  private fun String.extractSourceSetName(): SourceSetName {
    // All kapt configurations start with `kapt`
    //
    //  Config             SourceSet
    //  | kaptAndroidTest  | androidTest
    //  | kaptTest         | test
    //  | kapt             | main
    //  etc.
    if (this.startsWith(kapt.value)) {
      return removePrefix(kapt.value)
        .decapitalize()
        .asSourceSetName()
    }

    // All the base JVM configurations omit "main" from their configuration name
    //
    //  Config             SourceSet
    //  | api              | main
    //  | compileOnlyApi   | main
    //  | implementation   | main
    //  etc.
    val configType = mainConfigurationsCapitalized
      .find { this.endsWith(it) }
      ?: return asSourceSetName()

    // For any other configuration, the formula is $sourceSetName${baseConfigurationName.capitalize()}
    //
    //  Config                SourceSet
    //  | debugApi            | debug
    //  | releaseCompileOnly  | release
    //  | testImplementation  | test
    //  etc.
    return removeSuffix(configType)
      .decapitalize()
      .asSourceSetName()
  }

  /**
   * Returns the '-api' version of the current configuration.
   *
   * In Returns | api | api | implementation | api | compileOnly | api | testImplementation
   * | testApi | debug | debugApi | androidTestImplementation | androidTestApi
   *
   * @return for any main/common configuration, just returns `api`. For any
   *   other configuration, it returns the [SourceSetName] appended with `Api`.
   * @since 0.12.0
   */
  fun apiVariant(): ConfigurationName = toSourceSetName().apiConfig()

  /**
   * Returns the '-implementation' version of the current configuration.
   *
   * In Returns | implementation | implementation | implementation | implementation
   * | compileOnly | implementation | testImplementation | testImplementation | debug
   * | debugImplementation | androidTestImplementation | androidTestImplementation
   *
   * @return for any main/common configuration, just returns `implementation`. For any
   *   other configuration, it returns the [SourceSetName] appended with `Implementation`.
   * @since 0.12.0
   */
  fun implementationVariant(): ConfigurationName = toSourceSetName().implementationConfig()

  /**
   * Returns the 'kapt-' version of the current configuration.
   *
   * @return for any main/common configuration, just returns `kapt`. For any
   *   other configuration, it returns `kapt` appended with the [SourceSetName].
   * @since 0.12.0
   */
  fun kaptVariant(): ConfigurationName = toSourceSetName().kaptVariant()

  /**
   * @return true if the configuration is an `api` variant
   * @since 0.12.0
   */
  fun isApi(): Boolean = this == apiVariant()

  /**
   * @return true if the configuration is an `implementation` variant
   * @since 0.12.0
   */
  fun isImplementation(): Boolean = this == implementationVariant()

  /**
   * @return true if the configuration is a `kapt` variant
   * @since 0.12.0
   */
  fun isKapt(): Boolean = this == kaptVariant()

  override fun compareTo(other: ConfigurationName): Int {
    return value.compareTo(other.value)
  }

  override fun toString(): String = "(ConfigurationName) `$value`"

  companion object {

    /** name of the 'androidTestImplementation' configuration */
    val androidTestImplementation: ConfigurationName =
      ConfigurationName("androidTestImplementation")

    /** name of the 'annotationProcessor' configuration */
    val annotationProcessor: ConfigurationName = ConfigurationName("annotationProcessor")

    /** name of the 'anvil' configuration */
    val anvil: ConfigurationName = ConfigurationName("anvil")

    /** name of the 'api' configuration */
    val api: ConfigurationName = ConfigurationName("api")

    /** name of the 'compile' configuration */
    val compile: ConfigurationName = ConfigurationName("compile")

    /** name of the 'compileOnly' configuration */
    val compileOnly: ConfigurationName = ConfigurationName("compileOnly")

    /** name of the 'compileOnlyApi' configuration */
    val compileOnlyApi: ConfigurationName = ConfigurationName("compileOnlyApi")

    /** name of the 'implementation' configuration */
    val implementation: ConfigurationName = ConfigurationName("implementation")

    /** name of the 'kapt' configuration */
    val kapt: ConfigurationName = ConfigurationName("kapt")

    /** name of the 'kotlinCompilerPluginClasspathMain' configuration */
    val kotlinCompileClasspath: ConfigurationName =
      ConfigurationName("kotlinCompilerPluginClasspathMain")

    /** name of the 'ksp' configuration */
    val ksp: ConfigurationName = ConfigurationName("ksp")

    /** name of the 'runtime' configuration */
    val runtime: ConfigurationName = ConfigurationName("runtime")

    /** name of the 'runtimeOnly' configuration */
    val runtimeOnly: ConfigurationName = ConfigurationName("runtimeOnly")

    /** name of the 'testApi' configuration */
    val testApi: ConfigurationName = ConfigurationName("testApi")

    /** name of the 'testImplementation' configuration */
    val testImplementation: ConfigurationName = ConfigurationName("testImplementation")

    /** */
    val mainConfigurations: List<String> = listOf(
      api.value,
      compile.value,
      compileOnly.value,
      compileOnlyApi.value,
      implementation.value,
      kapt.value,
      // kotlinCompilerPluginClasspath is a special case,
      // since the main config is suffixed with "Main"
      kotlinCompileClasspath.value,
      runtime.value,
      runtimeOnly.value
    )
      // The order of this list matters. CompileOnlyApi must be before `api` or
      // `extractSourceSetName` below will match the wrong suffix.
      .sortedByDescending { it.length }

    internal val mainCommonConfigurations: List<String> = listOf(
      api.value,
      implementation.value
    )

    private val mainConfigurationsCapitalized: Set<String> = mainConfigurations
      .map { it.capitalize() }
      .toSet()

    /** the names of all configurations consumed by the main source set */
    fun main(): List<ConfigurationName> = listOf(
      compileOnlyApi,
      api,
      implementation,
      compileOnly,
      compile,
      kapt,
      runtimeOnly,
      runtime
    )

    /**
     * the base configurations which do not leak their transitive dependencies (basically not `api`)
     */
    fun private(): List<ConfigurationName> = listOf(
      implementation,
      compileOnly,
      compile,
      runtimeOnly,
      runtime
    )

    /**
     * the base configurations which include their dependencies as "compile" dependencies in the POM
     */
    fun public(): List<ConfigurationName> = listOf(
      compileOnlyApi,
      api
    )
  }
}

/** @return a ConfigurationName from this raw string */
fun String.asConfigurationName(): ConfigurationName = ConfigurationName(this)

fun <T : Any> Map<ConfigurationName, Collection<T>>.main(): List<T> {
  return listOfNotNull(
    get(ConfigurationName.api),
    get(ConfigurationName.compileOnly),
    get(ConfigurationName.implementation),
    get(ConfigurationName.runtimeOnly)
  ).flatten()
}

/** @return all source set names from this configuration names, without duplicates */
fun Iterable<ConfigurationName>.distinctSourceSetNames(): List<SourceSetName> =
  map { it.toSourceSetName() }
    .distinct()

/** @return all source set names from this configuration names, without duplicates */
fun Sequence<ConfigurationName>.distinctSourceSetNames(): Sequence<SourceSetName> =
  map { it.toSourceSetName() }
    .distinct()
