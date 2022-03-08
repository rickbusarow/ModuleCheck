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

package modulecheck.parsing.gradle

import modulecheck.utils.capitalize
import modulecheck.utils.decapitalize

class Configurations(
  delegate: Map<ConfigurationName, Config>
) : Map<ConfigurationName, Config> by delegate {

  override fun toString(): String {
    return toList().joinToString("\n")
  }
}

@JvmInline
value class ConfigurationName(val value: String) : Comparable<ConfigurationName> {

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
   */
  fun nameWithoutSourceSet() = value.removePrefix(toSourceSetName().value)

  /**
   * find the "base" configuration name and remove it
   *
   * For instance, `debugCompileOnly` would find the "CompileOnly" and remove it, returning "debug"
   * as the sourceSet name
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
   * In Returns | api | api | implementation | api | compileOnly | api | testImplementation |
   * testApi | debug | debugApi | androidTestImplementation | androidTestApi
   *
   * @return for any main/common configuration, just returns `api`. For any other configuration, it
   *   returns the [SourceSetName] appended with `Api`.
   */
  fun apiVariant() = toSourceSetName().apiConfig()

  /**
   * Returns the '-implementation' version of the current configuration.
   *
   * In Returns | implementation | implementation | implementation | implementation | compileOnly
   * | implementation | testImplementation | testImplementation | debug | debugImplementation |
   * androidTestImplementation | androidTestImplementation
   *
   * @return for any main/common configuration, just returns `implementation`. For any other
   *   configuration, it returns the [SourceSetName] appended with `Implementation`.
   */
  fun implementationVariant() = toSourceSetName().implementationConfig()

  fun isApi(): Boolean = this == apiVariant()
  fun isImplementation(): Boolean = this == implementationVariant()

  override fun compareTo(other: ConfigurationName): Int {
    return value.compareTo(other.value)
  }

  override fun toString(): String = "(ConfigurationName) `$value`"

  companion object {

    val androidTestImplementation = ConfigurationName("androidTestImplementation")
    val api = ConfigurationName("api")
    val compile = ConfigurationName("compile")
    val compileOnly = ConfigurationName("compileOnly")
    val compileOnlyApi = ConfigurationName("compileOnlyApi")
    val implementation = ConfigurationName("implementation")
    val kapt = ConfigurationName("kapt")
    val runtime = ConfigurationName("runtime")
    val runtimeOnly = ConfigurationName("runtimeOnly")
    val testApi = ConfigurationName("testApi")
    val testImplementation = ConfigurationName("testImplementation")

    /**
     * The order of this list matters. CompileOnlyApi must be before `api` or `extractSourceSetName`
     * below will match the wrong suffix.
     */
    internal val mainConfigurations = listOf(
      compileOnlyApi.value,
      api.value,
      kapt.value,
      implementation.value,
      compileOnly.value,
      compile.value,
      runtimeOnly.value,
      runtime.value
    )

    internal val mainCommonConfigurations = listOf(
      api.value,
      implementation.value
    )

    private val mainConfigurationsCapitalized = mainConfigurations
      .map { it.capitalize() }
      .toSet()

    fun main() = listOf(
      compileOnlyApi,
      api,
      implementation,
      compileOnly,
      compile,
      runtimeOnly,
      runtime
    )

    fun private() = listOf(
      implementation,
      compileOnly,
      compile,
      runtimeOnly,
      runtime
    )

    fun public() = listOf(
      compileOnlyApi,
      api
    )
  }
}

fun String.asConfigurationName(): ConfigurationName = ConfigurationName(this)

data class Config(
  val name: ConfigurationName,
  private val upstreamSequence: Sequence<Config>,
  private val downstreamSequence: Sequence<Config>
) {

  val upstream: List<Config> by lazy { upstreamSequence.toList() }
  val downstream: List<Config> by lazy { downstreamSequence.toList() }
  fun withUpstream(): List<Config> = listOf(this) + upstream
  fun withDownstream(): List<Config> = listOf(this) + downstream

  override fun toString(): String {
    return """Config   --  name=${name.value}
    |  upstream=${upstream.map { it.name.value }}
    |  downstream=${downstream.map { it.name.value }}
    |)
    """.trimMargin()
  }
}

fun <T : Any> Map<ConfigurationName, Collection<T>>.main(): List<T> {
  return listOfNotNull(
    get(ConfigurationName.api),
    get(ConfigurationName.compileOnly),
    get(ConfigurationName.implementation),
    get(ConfigurationName.runtimeOnly)
  ).flatten()
}

fun <K : Any, T : Any> Map<K, Collection<T>>.all(): List<T> {
  return values.flatten()
}

fun Iterable<Config>.names(): List<ConfigurationName> = map { it.name }
fun Sequence<Config>.names(): Sequence<ConfigurationName> = map { it.name }
