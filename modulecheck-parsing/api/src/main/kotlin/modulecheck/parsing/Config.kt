/*
 * Copyright (C) 2021 Rick Busarow
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

package modulecheck.parsing

@JvmInline
value class ConfigurationName(val value: String) : Comparable<ConfigurationName> {
  fun toSourceSetName(): SourceSetName = when (this.value) {
    // "main" source set configurations omit the "main" from their name,
    // creating "implementation" instead of "mainImplementation"
    in baseConfigurations -> SourceSetName.MAIN
    // all other configurations (like "test", "debug", or "androidTest")
    // are just "$sourceSetName${baseConfigurationName.capitalize()}"
    else -> this.value.extractSourceSetName()
  }

  /**
   * Returns the base name of the Configuration without any source set prefix.
   *
   * For "main" source sets, this function just returns the same string, e.g.:
   *   ConfigurationName("api").nameWithoutSourceSet() == "api"
   *   ConfigurationName("implementation").nameWithoutSourceSet() == "implementation"
   *
   * For other source sets, it returns the base configuration names:
   *   ConfigurationName("debugApi").nameWithoutSourceSet() == "Api"
   *   ConfigurationName("testImplementation").nameWithoutSourceSet() == "Implementation"
   */
  fun nameWithoutSourceSet() = value.removePrefix(toSourceSetName().value)

  /**
   * find the "base" configuration name and remove it
   *
   * For instance, `debugCompileOnly` would find the "CompileOnly" and remove it,
   * returning "debug" as the sourceSet name
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
        .toSourceSetName()
    }

    // All the base JVM configurations omit "main" from their configuration name
    //
    //  Config             SourceSet
    //  | api              | main
    //  | compileOnlyApi   | main
    //  | implementation   | main
    //  etc.
    val configType = baseConfigurationsCapitalized
      .find { this.endsWith(it) }
      ?: return toSourceSetName()

    // For any other configuration, the formula is $sourceSetName${baseConfigurationName.capitalize()}
    //
    //  Config                SourceSet
    //  | debugApi            | debug
    //  | releaseCompileOnly  | release
    //  | testImplementation  | test
    //  etc.
    return removeSuffix(configType)
      .decapitalize()
      .toSourceSetName()
  }

  /**
   * Returns the '-api' version of the current configuration.
   *
   * In                           Returns
   * | api                        | api
   * | implementation             | api
   * | compileOnly                | api
   * | testImplementation         | testApi
   * | debug                      | debugApi
   * | androidTestImplementation  | androidTestApi
   *
   * @return for any main/common configuration, just returns `api`. For any other configuration, it
   *   returns the [SourceSetName] appended with `Api`.
   */
  fun apiVariant() = toSourceSetName().apiConfig()

  fun isApi(): Boolean = this == apiVariant()

  override fun compareTo(other: ConfigurationName): Int {
    return value.compareTo(other.value)
  }

  override fun toString(): String = "ConfigurationName('$value')"

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
     * The order of this list matters.
     * CompileOnlyApi must be before `api` or `extractSourceSetName` below will match the wrong suffix.
     */
    internal val baseConfigurations = listOf(
      compileOnlyApi.value,
      api.value,
      kapt.value,
      implementation.value,
      compileOnly.value,
      compile.value,
      runtimeOnly.value,
      runtime.value
    )

    private val baseConfigurationsCapitalized = baseConfigurations
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
  val externalDependencies: Set<ExternalDependency>,
  val inherited: Set<Config>
)

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
