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

import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.ConfiguredDependency
import modulecheck.model.dependency.ExternalDependency
import modulecheck.model.dependency.MavenCoordinates
import modulecheck.model.dependency.ProjectDependency
import modulecheck.model.dependency.ProjectPath
import modulecheck.model.sourceset.SourceSetName
import modulecheck.model.sourceset.hasPrefix
import modulecheck.model.sourceset.removePrefix
import modulecheck.parsing.gradle.dsl.ProjectAccessor.TypeSafeProjectAccessor
import modulecheck.parsing.kotlin.compiler.internal.isKotlinScriptFile
import modulecheck.utils.findMinimumIndent
import modulecheck.utils.isGreaterThan
import modulecheck.utils.letIf
import modulecheck.utils.mapToSet
import modulecheck.utils.prefixIfNot
import modulecheck.utils.sortedWith

/**
 * Precompiled configuration names are names which are added by a pre-compiled plugin. These names
 * can be used as functions in Kotlin scripts. examples:
 * ```
 *   api("some-dependency")
 *   testImplementation(project(":my-lib"))
 *   kapt(libs.dagger)
 * ```
 *
 * If a configuration is added in a local build script, then it won't have a function associated
 * with it. In this case, the Kotlin script supports using a String extension function:
 * ```
 *   "internalReleaseApi"(libs.timber)
 * ```
 *
 * @param project the project in which the configuration name is being used
 * @return `true` if we can know for sure that it's pre-compiled. `false` if we aren't certain.
 * @receiver the configuration name which may have an accessor
 * @since 0.12.0
 */
suspend fun ConfigurationName.isDefinitelyPrecompiledForProject(
  project: HasDependencyDeclarations
): Boolean {

  return toSourceSetName().isDefinitelyPrecompiledForProject(project) ||
    project.getConfigurationInvocations().contains(value)
}

/**
 * Creates a new [DependencyDeclaration] which can be added to a build file, potentially using a
 * similar existing declaration as a template.
 *
 * @param project
 * @return a Pair where the first declaration is the newly created one, and the second is the
 *     pre-existing template, or null if a template was not used.
 * @since 0.13.0
 */
suspend fun ConfiguredDependency.asDeclaration(
  project: HasDependencyDeclarations
): Pair<DependencyDeclaration, DependencyDeclaration?> {

  val tokenOrNull = project.closestDeclarationOrNull(
    newDependency = this,
    matchPathFirst = true
  )

  val newDeclaration = when (val newDependency = this) {
    is ProjectDependency -> newDependency.asModuleDependencyDeclaration(project, tokenOrNull)
    is ExternalDependency -> newDependency.asExternalDependencyDeclaration(tokenOrNull, project)
  }

  return newDeclaration to tokenOrNull
}

/**
 * Finds the existing dependency declaration (if there are any) which is the closest match to the
 * desired new dependency.
 *
 * @param newDependency The dependency being added
 * @param matchPathFirst If true, matching project paths will be prioritized over matching
 *     configurations. If false, configuration matches will take priority over a matching project
 *     path.
 * @receiver the project containing this declaration's match
 * @return the closest matching declaration, or null if there are no declarations at all.
 * @since 0.12.0
 */
suspend fun HasDependencyDeclarations.closestDeclarationOrNull(
  newDependency: ConfiguredDependency,
  matchPathFirst: Boolean
): DependencyDeclaration? {

  return buildFileParser.dependenciesBlocks()
    .firstNotNullOfOrNull { dependenciesBlock ->

      val sorted = dependenciesBlock.settings
        .filterNot { it is UnknownDependencyDeclaration }
        .sorted(matchPathFirst, newDependency)

      val closestDeclaration = sorted.firstOrNull {
        when (newDependency) {
          is ExternalDependency -> it.mavenCoordinatesOrNull() == newDependency.mavenCoordinates
          is ProjectDependency -> it.projectPathOrNull() == newDependency.path
        }
      }
        ?: sorted.firstOrNull {

          when (newDependency) {
            is ExternalDependency -> it.mavenCoordinatesOrNull()
              ?.isGreaterThan(newDependency.mavenCoordinates)

            is ProjectDependency -> it.projectPathOrNull()
              ?.isGreaterThan(newDependency.path)
          } ?: false
        }
        ?: sorted.lastOrNull()
        ?: return@firstNotNullOfOrNull null

      val sameDependency = when (newDependency) {
        is ExternalDependency -> {
          closestDeclaration.mavenCoordinatesOrNull() == newDependency.mavenCoordinates
        }

        is ProjectDependency -> {
          closestDeclaration.projectPathOrNull() == newDependency.path
        }
      }

      if (sameDependency) {
        closestDeclaration
      } else {

        val precedingWhitespace = "^\\s*".toRegex()
          .find(closestDeclaration.statementWithSurroundingText)?.value ?: ""

        when (closestDeclaration) {
          is ExternalDependencyDeclaration -> closestDeclaration.copy(
            statementWithSurroundingText = closestDeclaration.statementWithSurroundingText
              .prefixIfNot(precedingWhitespace),
            suppressed = emptyList()
          )

          is ModuleDependencyDeclaration -> closestDeclaration.copy(
            statementWithSurroundingText = closestDeclaration.statementWithSurroundingText
              .prefixIfNot(precedingWhitespace),
            suppressed = emptyList()
          )

          is UnknownDependencyDeclaration -> {
            // this shouldn't actually be possible
            null
          }
        }
      }
    }
}

private fun List<DependencyDeclaration>.sorted(
  matchPathFirst: Boolean,
  newDependency: ConfiguredDependency
) = sortedWith(
  {
    if (matchPathFirst) {
      it.configName == newDependency.configurationName
    } else {
      it.configName != newDependency.configurationName
    }
  },
  { it !is ModuleDependencyDeclaration },
  {
    // sort by module paths, but normalize between String paths and type-safe accessors.
    // String paths will start with ":", so remove that prefix.
    // Type-safe accessors will have "." separators, so replace those with ":".
    // After that, everything should read like `foo:bar:baz`.
    (it as? ModuleDependencyDeclaration)
      ?.projectPath
      ?.value
      ?.removePrefix(":")
      ?.replace(".", ":")
      ?: (it as? ExternalDependencyDeclaration)
        ?.coordinates
        ?.name
      ?: ""
  }
)

private fun DependencyDeclaration.projectPathOrNull(): ProjectPath? {
  return (this as? ModuleDependencyDeclaration)?.projectPath
}

private fun DependencyDeclaration.mavenCoordinatesOrNull(): MavenCoordinates? {
  return (this as? ExternalDependencyDeclaration)?.coordinates
}

private suspend fun ProjectDependency.asModuleDependencyDeclaration(
  project: HasDependencyDeclarations,
  tokenOrNull: DependencyDeclaration?
): ModuleDependencyDeclaration {
  return if (tokenOrNull is ModuleDependencyDeclaration) {
    tokenOrNull.copy(
      newConfigName = configurationName,
      newModulePath = path,
      testFixtures = isTestFixture
    )
  } else {
    project.createDependencyDeclaration(
      configurationName = configurationName,
      projectPath = path,
      isTestFixtures = isTestFixture
    )
  }
}

private suspend fun ExternalDependency.asExternalDependencyDeclaration(
  tokenOrNull: DependencyDeclaration?,
  project: HasDependencyDeclarations
): ExternalDependencyDeclaration {
  return if (tokenOrNull is ExternalDependencyDeclaration) {
    tokenOrNull.copy(
      newConfigName = configurationName,
      newCoordinates = mavenCoordinates,
      testFixtures = isTestFixture
    )
  } else {
    project.createDependencyDeclaration(
      configurationName = configurationName,
      mavenCoordinates = mavenCoordinates,
      isTestFixtures = isTestFixture
    )
  }
}

/**
 * Creates a new [ModuleDependencyDeclaration] from the void, without copying the style of any other
 * dependency declarations.
 *
 * This does not automatically write the dependency to the build file or add it to any collections.
 *
 * @param configurationName the new config name
 * @param projectPath the new project dependency
 * @param isTestFixtures if true, the dependency is wrapped in `testFixtures(...)`, like
 *     `api(testFixtures(project(":lib1")))`
 * @receiver the project receiving this new dependency
 * @return a new declaration model
 * @since 0.13.0
 */
suspend fun HasDependencyDeclarations.createDependencyDeclaration(
  configurationName: ConfigurationName,
  projectPath: ProjectPath,
  isTestFixtures: Boolean
): ModuleDependencyDeclaration {

  val isKotlin = buildFile.isKotlinScriptFile()

  val configInvocation = getConfigInvocation(isKotlin, configurationName)

  val accessorText = projectAccessors()
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
  val projectAccessor = ProjectAccessor.from(accessorText, projectPath = projectPath)
  val (declarationText, statementWithSurroundingText) = getStatementWithSurroundingText(
    accessorText = accessorText,
    isTestFixtures = isTestFixtures,
    isKotlin = isKotlin,
    configInvocation = configInvocation
  )
  return ModuleDependencyDeclaration(
    projectPath = projectPath,
    projectAccessor = projectAccessor,
    configName = configurationName,
    declarationText = declarationText,
    statementWithSurroundingText = statementWithSurroundingText,
    suppressed = emptyList()
  ) { it.value }
}

private suspend fun HasDependencyDeclarations.getConfigInvocation(
  isKotlin: Boolean,
  configurationName: ConfigurationName
): String {
  return if (isKotlin && !configurationName.isDefinitelyPrecompiledForProject(project = this)) {
    configurationName.wrapInQuotes()
  } else {
    configurationName.value
  }
}

/**
 * Creates a new [ExternalDependencyDeclaration] from the void, without copying the style of any
 * other dependency declarations.
 *
 * This does not automatically write the dependency to the build file or add it to any collections.
 *
 * @param configurationName the new config name
 * @param mavenCoordinates the new dependency
 * @param isTestFixtures if true, the dependency is wrapped in `testFixtures(...)`, like
 *     `api(testFixtures("com.example:foo:1:))`
 * @receiver the project receiving this new dependency
 * @return a new declaration model
 * @since 0.13.0
 */
suspend fun HasDependencyDeclarations.createDependencyDeclaration(
  configurationName: ConfigurationName,
  mavenCoordinates: MavenCoordinates,
  isTestFixtures: Boolean
): ExternalDependencyDeclaration {

  val isKotlin = buildFile.isKotlinScriptFile()

  val configInvocation = getConfigInvocation(isKotlin, configurationName)

  val accessorText = if (isKotlin) {
    "\"${mavenCoordinates.name}\""
  } else {
    "'${mavenCoordinates.name}'"
  }
  val (declarationText, statementWithSurroundingText) = getStatementWithSurroundingText(
    accessorText = accessorText,
    isTestFixtures = isTestFixtures,
    isKotlin = isKotlin,
    configInvocation = configInvocation
  )
  return ExternalDependencyDeclaration(
    configName = configurationName,
    declarationText = declarationText,
    statementWithSurroundingText = statementWithSurroundingText,
    suppressed = emptyList(),
    configurationNameTransform = { it.value },
    group = mavenCoordinates.group,
    moduleName = mavenCoordinates.moduleName, version = mavenCoordinates.version,
    coordinates = mavenCoordinates
  )
}

private suspend fun HasDependencyDeclarations.getStatementWithSurroundingText(
  accessorText: String,
  isTestFixtures: Boolean,
  isKotlin: Boolean,
  configInvocation: String
): Pair<String, String> {

  val projectWithTestFixtures = accessorText
    .letIf(isTestFixtures) { "testFixtures($it)" }

  val declarationText = if (isKotlin) {
    "$configInvocation($projectWithTestFixtures)"
  } else {
    "$configInvocation $projectWithTestFixtures"
  }

  return declarationText to buildFileParser.dependenciesBlocks()
    .map { it.lambdaContent.findMinimumIndent() }
    .minByOrNull { it.length }
    .let { min ->
      val indent = min ?: "  "
      "$indent$declarationText\n"
    }
}

private tailrec fun SourceSetName.isDefinitelyPrecompiledForProject(
  project: HasDependencyDeclarations
): Boolean {

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
        return removePrefix(SourceSetName.DEBUG).isDefinitelyPrecompiledForProject(project)
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
 * @since 0.12.0
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
 *
 * @since 0.12.0
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
