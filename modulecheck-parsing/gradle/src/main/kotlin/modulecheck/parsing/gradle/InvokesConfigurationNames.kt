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

import java.io.File

interface InvokesConfigurationNames :
  PluginAware,
  HasBuildFile,
  HasConfigurations,
  HasDependencyDeclarations

interface PluginAware {

  val hasKapt: Boolean
  val hasTestFixturesPlugin: Boolean
  val hasAnvil: Boolean
  val hasAGP: Boolean
}

interface HasBuildFile {
  val buildFile: File
}

interface HasDependencyDeclarations : HasBuildFile, HasConfigurations {

  fun getConfigurationInvocations(): Set<String>
}

interface HasPath {
  val path: String
}

interface HasConfigurations {
  val sourceSets: SourceSets
  val configurations: Configurations
}

/**
 * Reverse lookup of all the configurations which inherit another configuration.
 *
 * For instance, every java/kotlin configuration (`implementation`, `testImplementation`, etc.)
 * within a project inherits from the common `api` configuration,
 * so `someProject.inheritingConfigurations(ConfigurationName.api)` would return all other
 * java/kotlin configurations within that project.
 */
fun HasConfigurations.inheritingConfigurations(configurationName: ConfigurationName): Set<Config> {
  return configurations.values
    .asSequence()
    .map { it.name.toSourceSetName() }
    .flatMap { sourceSet ->
      sourceSet.javaConfigurationNames()
        .mapNotNull { configName -> configurations[configName] }
    }
    .filter { inheritingConfig ->
      inheritingConfig.inherited
        .any { inheritedConfig ->
          inheritedConfig.name == configurationName
        }
    }.toSet()
}

/**
 * Precompiled configuration names are names which are added by a pre-compiled plugin.  These names
 * can be used as functions in Kotlin scripts.
 * examples:
 * ```
 *   api("some-dependency")
 *   testImplementation(project(":my-lib"))
 *   kapt(libs.dagger)
 * ```
 *
 * If a configuration is added in a local build script, then it won't have a function associated
 * with it.  In this case, the Kotlin script supports using a String extension function:
 * ```
 *   "internalReleaseApi"(libs.timber)
 * ```
 * @param project the project in which the configuration name is being used
 * @return `true` if we can know for sure that it's pre-compiled.  `false` if we aren't certain.
 */
fun <T> ConfigurationName.isDefinitelyPrecompiledForProject(project: T): Boolean
  where T : PluginAware,
        T : HasDependencyDeclarations {
  return when (toSourceSetName()) {
    SourceSetName.ANVIL -> project.hasAnvil
    SourceSetName.MAIN -> true
    SourceSetName.TEST -> true
    SourceSetName.TEST_FIXTURES -> project.hasTestFixturesPlugin
    SourceSetName.KAPT -> project.hasKapt
    SourceSetName.DEBUG -> project.hasAGP
    SourceSetName.RELEASE -> project.hasAGP
    SourceSetName.ANDROID_TEST -> project.hasAGP
    else -> return project.getConfigurationInvocations().contains(value)
  }
}

/**
 * Attempts to determine the most idiomatic way of invoking the receiver
 * [configuration name][ConfigurationName].  Typically, this will just be a function with a matching
 * name.  However, if a configuration is non-standard (e.g. `internalReleaseImplementation`) and the
 * build file is using the Kotlin Gradle DSL, then the configuration must be invoked as a String
 * extension function instead (e.g. `"internalReleaseImplementation"(libs.myDependency)`).
 *
 * @return The text used to add a dependency using this [ConfigurationName], in this project.
 * @see isDefinitelyPrecompiledForProject
 */
fun ConfigurationName.buildFileInvocationText(
  invokesConfigurationNames: InvokesConfigurationNames
): String {

  val buildFileIsKotlin = invokesConfigurationNames.buildFile.extension == "kts"

  return if (buildFileIsKotlin && !isDefinitelyPrecompiledForProject(invokesConfigurationNames)) {
    wrapInQuotes()
  } else {
    value
  }
}

private fun ConfigurationName.wrapInQuotes(): String =
  value.let { if (it.endsWith('"')) it else "$it\"" }
    .let { if (it.startsWith('"')) it else "\"$it" }
