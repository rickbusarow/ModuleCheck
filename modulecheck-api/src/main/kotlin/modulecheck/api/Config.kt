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

package modulecheck.api

data class ConfigurationName(val value: String) : Comparable<ConfigurationName> {
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
      @Suppress("DEPRECATION") // we have to use `decapitalize()` for compatibility with Kotlin 1.4.x and Gradle < 7.0
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
    @Suppress("DEPRECATION") // we have to use `decapitalize()` for compatibility with Kotlin 1.4.x and Gradle < 7.0
    return removeSuffix(configType)
      .decapitalize()
      .toSourceSetName()
  }

  override fun compareTo(other: ConfigurationName): Int {
    return value.compareTo(other.value)
  }

  companion object {

    val compileOnlyApi = ConfigurationName("compileOnlyApi")
    val api = ConfigurationName("api")
    val kapt = ConfigurationName("kapt")
    val implementation = ConfigurationName("implementation")
    val compileOnly = ConfigurationName("compileOnly")
    val compile = ConfigurationName("compile")
    val runtimeOnly = ConfigurationName("runtimeOnly")
    val runtime = ConfigurationName("runtime")

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

    @Suppress("DEPRECATION") // we have to use `capitalize()` for compatibility with Kotlin 1.4.x and Gradle < 7.0
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
    get("api".asConfigurationName()),
    get("compileOnly".asConfigurationName()),
    get("implementation".asConfigurationName()),
    get("runtimeOnly".asConfigurationName())
  ).flatten()
}

fun <K : Any, T : Any> Map<K, Collection<T>>.all(): List<T> {
  return values.flatten()
}
