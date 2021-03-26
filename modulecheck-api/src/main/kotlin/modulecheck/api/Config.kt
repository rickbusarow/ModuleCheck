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

data class ConfigurationName(val value: String) {
  fun asSourceSetName(): SourceSetName = when (this.value) {
    // "main" source set configurations omit the "main" from their name,
    // creating "implementation" instead of "mainImplementation"
    in baseConfigurations -> "main".asSourceSetName()
    // all other configurations (like "test", "debug", or "androidTest")
    // are just "$sourceSetName${configurationName.capitalize()}"
    else -> this.value.extractSourceSetName()
  }

  /**
   * find the "base" configuration name and remove it
   *
   * For instance, `debugCompileOnly` would find the "CompileOnly" and remove it,
   * returning "debug" as the sourceSet name
   */
  private fun String.extractSourceSetName(): SourceSetName {
    val configType = baseConfigurationsCapitalized
      .find { this.endsWith(it) }
      ?: return asSourceSetName()

    return removeSuffix(configType).asSourceSetName()
  }

  companion object {

    val compileOnlyApi = ConfigurationName("compileOnlyApi")
    val api = ConfigurationName("api")
    val implementation = ConfigurationName("implementation")
    val compileOnly = ConfigurationName("compileOnly")
    val compile = ConfigurationName("compile")
    val runtimeOnly = ConfigurationName("runtimeOnly")
    val runtime = ConfigurationName("runtime")

    /**
     * The order of this list matters.
     * CompileOnlyApi must be before `api` or `extractSourceSetName` below will match the wrong suffix.
     */
    private val baseConfigurations = listOf(
      compileOnlyApi.value,
      api.value,
      implementation.value,
      compileOnly.value,
      compile.value,
      runtimeOnly.value,
      runtime.value
    )
    private val baseConfigurationsCapitalized = baseConfigurations
      .map { it.capitalize() }
      .toSet()
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
