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

package modulecheck.parsing.gradle.dsl.internal

import modulecheck.finding.FindingName
import modulecheck.finding.FindingName.Companion.migrateLegacyIdOrNull
import modulecheck.model.dependency.ProjectDependency
import modulecheck.parsing.gradle.dsl.DependenciesBlock
import modulecheck.parsing.gradle.dsl.DependencyDeclaration
import modulecheck.parsing.gradle.dsl.DependencyDeclaration.ConfigurationNameTransform
import modulecheck.parsing.gradle.dsl.ExternalDependencyDeclaration
import modulecheck.parsing.gradle.dsl.InvokesConfigurationNames
import modulecheck.parsing.gradle.dsl.ModuleDependencyDeclaration
import modulecheck.parsing.gradle.dsl.ProjectAccessor
import modulecheck.parsing.gradle.dsl.UnknownDependencyDeclaration
import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.parsing.gradle.model.MavenCoordinates
import modulecheck.parsing.gradle.model.ProjectPath
import modulecheck.parsing.gradle.model.ProjectPath.StringProjectPath
import modulecheck.reporting.logging.McLogger
import modulecheck.utils.lazy.ResetManager
import modulecheck.utils.lazy.lazyResets
import modulecheck.utils.remove

abstract class AbstractDependenciesBlock(
  private val logger: McLogger,
  blockSuppressed: List<String>,
  private val configurationNameTransform: ConfigurationNameTransform,
  private val projectDependency: ProjectDependency.Factory
) : DependenciesBlock {

  private val resetManager = ResetManager()

  final override val blockSuppressed = blockSuppressed.updateOldSuppresses()

  override val allSuppressions: Map<ProjectDependency, Set<FindingName>> by resetManager.lazyResets {
    buildMap<ProjectDependency, MutableSet<FindingName>> {

      allModuleDeclarations.forEach { (configuredModule, declarations) ->

        val cached = getOrPut(configuredModule) {
          blockSuppressed
            .mapNotNull { FindingName.safe(it) }
            .mapTo(mutableSetOf()) { it }
        }

        declarations.forEach { moduleDependencyDeclaration ->

          cached += moduleDependencyDeclaration.suppressed.updateOldSuppresses()
            .plus(blockSuppressed)
            .asFindingNames()
        }
      }
    }
  }

  private val originalLines by lazy { lambdaContent.lines().toMutableList() }

  private val _allDeclarations = mutableListOf<DependencyDeclaration>()

  override val settings: List<DependencyDeclaration>
    get() = _allDeclarations

  private val allExternalDeclarations =
    mutableMapOf<MavenCoordinates, MutableList<ExternalDependencyDeclaration>>()

  private val allModuleDeclarations =
    mutableMapOf<ProjectDependency, MutableList<ModuleDependencyDeclaration>>()

  fun addNonModuleStatement(
    configName: ConfigurationName,
    parsedString: String,
    coordinates: MavenCoordinates,
    suppressed: List<String>
  ) {
    val originalString = getOriginalString(parsedString)

    val declaration = ExternalDependencyDeclaration(
      configName = configName,
      declarationText = parsedString,
      statementWithSurroundingText = originalString,
      group = coordinates.group,
      moduleName = coordinates.moduleName,
      version = coordinates.version,
      coordinates = coordinates,
      suppressed = suppressed.updateOldSuppresses() + blockSuppressed,
      configurationNameTransform = configurationNameTransform
    )
    _allDeclarations.add(declaration)
    allExternalDeclarations.getOrPut(coordinates) { mutableListOf() }
      .add(declaration)
  }

  fun addUnknownStatement(
    configName: ConfigurationName,
    parsedString: String,
    argument: String,
    suppressed: List<String>
  ) {
    val originalString = getOriginalString(parsedString)

    val declaration = UnknownDependencyDeclaration(
      argument = argument,
      configName = configName,
      declarationText = parsedString,
      statementWithSurroundingText = originalString,
      suppressed = suppressed.updateOldSuppresses() + blockSuppressed,
      configurationNameTransform = configurationNameTransform
    )
    _allDeclarations.add(declaration)
  }

  /**
   * @param projectPath `:my:project:lib1` or `my.project.lib1`
   * @param projectAccessor `project(:my:project:lib1)` or `projects.my.project.lib1`
   * @since 0.12.0
   */
  fun addModuleStatement(
    configName: ConfigurationName,
    parsedString: String,
    projectPath: ProjectPath,
    projectAccessor: ProjectAccessor,
    suppressed: List<String>
  ) {

    val isTestFixtures = parsedString.contains(testFixturesRegex)

    val cpd = projectDependency.create(
      configurationName = configName,
      path = projectPath,
      isTestFixture = isTestFixtures
    )

    val originalString = getOriginalString(parsedString)

    val declaration = ModuleDependencyDeclaration(
      projectPath = projectPath,
      projectAccessor = projectAccessor,
      configName = configName,
      declarationText = parsedString,
      statementWithSurroundingText = originalString,
      suppressed = suppressed.updateOldSuppresses() + blockSuppressed,
      configurationNameTransform = configurationNameTransform
    )

    allModuleDeclarations.getOrPut(cpd) { mutableListOf() }
      .add(declaration)

    _allDeclarations.add(declaration)

    resetManager.resetAll()
  }

  private fun List<String>.updateOldSuppresses(): List<String> {
    @Suppress("DEPRECATION")
    return map { originalName ->
      migrateLegacyIdOrNull(originalName, logger) ?: originalName
    }
  }

  private fun Collection<String>.asFindingNames(): Set<FindingName> {
    return mapNotNull { FindingName.safe(it) }.toSet()
  }

  override fun getOrEmpty(
    moduleRef: String,
    configName: ConfigurationName,
    testFixtures: Boolean
  ): List<ModuleDependencyDeclaration> {
    return getOrEmpty(StringProjectPath(moduleRef), configName, testFixtures)
  }

  override fun getOrEmpty(
    moduleRef: ProjectPath,
    configName: ConfigurationName,
    testFixtures: Boolean
  ): List<ModuleDependencyDeclaration> {

    return allModuleDeclarations[
      projectDependency.create(
        configurationName = configName,
        path = moduleRef,
        isTestFixture = testFixtures
      )
    ].orEmpty()
  }

  override fun getOrEmpty(
    mavenCoordinates: MavenCoordinates,
    configName: ConfigurationName
  ): List<ExternalDependencyDeclaration> {
    return allExternalDeclarations[mavenCoordinates]
      ?.filter { it.configName == configName }
      .orEmpty()
  }

  /**
   * Compares the target parsed string to the un-parsed lines of the original dependencies block,
   * and returns the index of **the last row** which matches the parsed string.
   *
   * So, given the target:
   *
   * ```
   * api(projects.foo.bar) {
   *   exclude(group = "androidx.appcompat")
   * }
   * ```
   *
   * And given the dependencies lines:
   *
   * ```
   * <blank line>
   * // Remove leaking AppCompat dependency
   * api(projects.foo.bar) {
   *   exclude(group = "androidx.appcompat")
   * }                                                // this is index 4
   * api(libs.junit)
   * ```
   *
   * This function would return index `4`, because rows 2-4 match the target parsed string.
   *
   * From this value, [getOriginalString] will return a multi-line string which includes the blank
   * line and the comment.
   *
   * @since 0.12.0
   */
  private fun findLastMatchingRowIndex(parsedString: String): Int {
    val targetLines = parsedString.lines()
      .map { it.trimStart() }

    // index is incremented at least once (if the first line is a match), so start at -1
    var index = -1

    var matched: Boolean

    do {
      val candidates = originalLines
        .drop(index + 1)
        .take(targetLines.size)
        .map { it.trimStart() }

      matched = candidates.zip(targetLines)
        .all { (candidate, target) ->
          originalLineMatchesParsed(candidate, target)
        }

      if (matched) {
        index += targetLines.size
      } else {

        index++
      }
    } while (!matched)

    return index
  }

  protected abstract fun originalLineMatchesParsed(
    originalLine: String,
    parsedString: String
  ): Boolean

  private fun getOriginalString(parsedString: String): String {
    val originalStringIndex = findLastMatchingRowIndex(parsedString)

    val originalStringLines = List(originalStringIndex + 1) {
      originalLines.removeFirst()
    }

    return originalStringLines.joinToString("\n")
      .remove("""[\s\S]*\/\/ ModuleCheck finding.*(?:\r\n|\r|\n)""".toRegex())
  }

  companion object {
    val testFixturesRegex = "testFixtures\\([\\s\\S]*\\)".toRegex()
  }
}

interface DependenciesBlocksProvider {

  suspend fun get(): List<DependenciesBlock>

  fun interface Factory {
    fun create(invokesConfigurationNames: InvokesConfigurationNames): DependenciesBlocksProvider
  }
}
