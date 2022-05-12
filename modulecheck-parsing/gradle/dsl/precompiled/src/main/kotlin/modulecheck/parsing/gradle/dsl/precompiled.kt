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

package modulecheck.parsing.gradle.dsl

import modulecheck.parsing.gradle.dsl.ProjectAccessor.TypeSafeProjectAccessor
import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.parsing.gradle.model.PluginAware
import modulecheck.parsing.gradle.model.ProjectPath
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.gradle.model.SourceSetName.Companion
import modulecheck.parsing.gradle.model.hasPrefix
import modulecheck.parsing.gradle.model.removePrefix
import modulecheck.utils.findMinimumIndent
import modulecheck.utils.letIf
import modulecheck.utils.mapToSet

/**
 * Precompiled configuration names are names which are added by a pre-compiled plugin. These names
 * can be used as functions in Kotlin scripts. examples:
 *
 * ```
 *   api("some-dependency")
 *   testImplementation(project(":my-lib"))
 *   kapt(libs.dagger)
 * ```
 *
 * If a configuration is added in a local build script, then it won't have a function associated
 * with it. In this case, the Kotlin script supports using a String extension function:
 *
 * ```
 *   "internalReleaseApi"(libs.timber)
 * ```
 *
 * @param project the project in which the configuration name is being used
 * @return `true` if we can know for sure that it's pre-compiled. `false` if we aren't certain.
 */
suspend fun <T> ConfigurationName.isDefinitelyPrecompiledForProject(project: T): Boolean
  where T : PluginAware,
        T : HasDependencyDeclarations {

  return toSourceSetName().isDefinitelyPrecompiledForProject(project) ||
    project.getConfigurationInvocations().contains(value)
}

suspend fun <T> T.createProjectDependencyDeclaration(
  configurationName: ConfigurationName,
  projectPath: ProjectPath,
  isTestFixtures: Boolean
): ModuleDependencyDeclaration
  where T : PluginAware,
        T : HasDependencyDeclarations {

  val isKotlin = buildFile.extension == "kts"

  val configInvocation = when {
    isKotlin && !configurationName.isDefinitelyPrecompiledForProject(this) -> {
      configurationName.wrapInQuotes()
    }
    else -> configurationName.value
  }

  val projectAccessorText = projectAccessors()
    .any { it is TypeSafeProjectAccessor }
    .let { useTypeSafe ->

      if (useTypeSafe) {
        "projects.${projectPath.typeSafeValue}"
      } else if (isKotlin) {
        "project(\"${projectPath.value}\")"
      } else {
        "project('${projectPath.value}')"
      }
    }

  val projectAccessor = ProjectAccessor.from(projectAccessorText, projectPath)

  val projectWithTestFixtures = projectAccessorText
    .letIf(isTestFixtures) { "testFixtures($it)" }

  val declarationText = if (isKotlin) {
    "$configInvocation($projectWithTestFixtures)"
  } else "$configInvocation $projectWithTestFixtures"

  val statementWithSurroundingText = buildFileParser.dependenciesBlocks()
    .map { it.lambdaContent.findMinimumIndent() }
    .minByOrNull { it.length }
    .let { min ->
      val indent = min ?: "  "
      "$indent$declarationText\n"
    }

  return ModuleDependencyDeclaration(
    projectPath,
    projectAccessor,
    configurationName,
    declarationText,
    statementWithSurroundingText,
    emptyList()
  ) { it.value }
}

private tailrec fun <T> SourceSetName.isDefinitelyPrecompiledForProject(project: T): Boolean
  where T : PluginAware,
        T : HasDependencyDeclarations {

  // simple cases
  when (this) {
    SourceSetName.ANVIL -> return project.hasAnvil
    SourceSetName.MAIN -> return true
    SourceSetName.TEST -> return true
    SourceSetName.TEST_FIXTURES -> return project.hasTestFixturesPlugin
    SourceSetName.KAPT -> return project.hasKapt
    SourceSetName.DEBUG -> return project.hasAGP
    SourceSetName.RELEASE -> return project.hasAGP
    SourceSetName.ANDROID_TEST -> return project.hasAGP
  }

  if (project.hasAnvil && hasPrefix(SourceSetName.ANVIL)) {
    // `anvilDebug` -> `debug`, then we'd recurse and check for `debug`.
    return removePrefix(SourceSetName.ANVIL).isDefinitelyPrecompiledForProject(project)
  }
  if (project.hasKapt && hasPrefix(SourceSetName.KAPT)) {
    // `kaptAndroidTest` -> `androidTest`, then we'd recurse and check for `androidTest`.
    return removePrefix(SourceSetName.KAPT).isDefinitelyPrecompiledForProject(project)
  }
  // Note that the `testFixtures` check has to be above anything dealing with a "test-" prefix.
  if (project.hasTestFixturesPlugin && hasPrefix(SourceSetName.TEST_FIXTURES)) {
    // `testFixturesDebug` -> `debug`, then we'd recurse and check for `debug`.
    return removePrefix(SourceSetName.TEST_FIXTURES).isDefinitelyPrecompiledForProject(project)
  }
  // `test` must come before Android stuff, because the source set is `testDebug` -- not `debugTest`
  if (hasPrefix(SourceSetName.TEST)) {
    return removePrefix(SourceSetName.TEST).isDefinitelyPrecompiledForProject(project)
  }
  if (project.hasAGP) {
    when {
      // `androidTest` MUST come before `debug` and `release`,
      // because the source set is `androidTest____`
      hasPrefix(SourceSetName.ANDROID_TEST) -> {
        return removePrefix(SourceSetName.ANDROID_TEST).isDefinitelyPrecompiledForProject(project)
      }
      hasPrefix(SourceSetName.DEBUG) -> {
        return removePrefix(Companion.DEBUG).isDefinitelyPrecompiledForProject(project)
      }
      hasPrefix(SourceSetName.RELEASE) -> {
        return removePrefix(SourceSetName.RELEASE).isDefinitelyPrecompiledForProject(project)
      }
    }
  }
  return false
}

/**
 * Attempts to determine the most idiomatic way of invoking the receiver
 * [configuration name][ConfigurationName]. Typically, this will just be a function with a matching
 * name. However, if a configuration is non-standard (e.g. `internalReleaseImplementation`) and
 * the build file is using the Kotlin Gradle DSL, then the configuration must be invoked as a
 * String extension function instead (e.g. `"internalReleaseImplementation"(libs.myDependency)`).
 *
 * @return The text used to add a dependency using this [ConfigurationName], in this project.
 * @see isDefinitelyPrecompiledForProject
 */
suspend fun ConfigurationName.buildFileInvocationText(
  invokesConfigurationNames: InvokesConfigurationNames
): String {

  return if (shouldUseQuotes(invokesConfigurationNames)) {
    wrapInQuotes()
  } else {
    value
  }
}

private fun ConfigurationName.wrapInQuotes(): String =
  value.let { if (it.endsWith('"')) it else "$it\"" }
    .let { if (it.startsWith('"')) it else "\"$it" }

/**
 * Returns true if the build file is Kotlin, and one of:
 * - this exact configuration name is already used as a string extension
 * - this configuration name is atypical (such as `internalDebugApi`) and not already used as a
 *   non-string invocation, so there's no way to be sure that the function is precompiled.
 */
private suspend fun ConfigurationName.shouldUseQuotes(
  invokesConfigurationNames: InvokesConfigurationNames
): Boolean {

  // This only applies to Kotlin DSL build files
  if (invokesConfigurationNames.buildFile.extension != "kts") return false

  // true if the build file *already* uses this exact same configuration as a String extension
  if (invokesConfigurationNames.getConfigurationInvocations().contains("\"$value\"")) {
    return true
  }

  // true if we can't find a plugin which creates this config, and we can't already find it being
  // used as a normal function invocation
  return !isDefinitelyPrecompiledForProject(invokesConfigurationNames)
}

private suspend fun HasBuildFile.projectAccessors(): Set<ProjectAccessor> {
  return buildFileParser.dependenciesBlocks()
    .flatMap { it.settings }
    .filterIsInstance<ModuleDependencyDeclaration>()
    .mapToSet { declaration -> declaration.projectAccessor }
}
