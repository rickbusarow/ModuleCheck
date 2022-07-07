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

package modulecheck.model.dependency

import modulecheck.config.CodeGeneratorBinding
import modulecheck.config.MightHaveCodeGeneratorBinding
import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.parsing.gradle.model.HasPath
import modulecheck.parsing.gradle.model.ProjectPath
import modulecheck.parsing.gradle.model.SourceSetName

/** Represents a specific dependency upon an internal project dependency. */
sealed class ProjectDependency : ConfiguredDependency, HasPath {

  /** name == path */
  override val identifier get() = path

  /**
   * The typical implementation of [ProjectDependency], for normal JVM or Android dependencies.
   *
   * @property configurationName the configuration used
   * @property path the path of the dependency project
   * @property isTestFixture Is the dependency being invoked via `testFixtures(project(...))`?
   */
  class RuntimeProjectDependency(
    override val configurationName: ConfigurationName,
    override val path: ProjectPath,
    override val isTestFixture: Boolean
  ) : ProjectDependency()

  /**
   * The implementation of [ProjectDependency] used for code-generator dependencies.
   *
   * @property configurationName the configuration used
   * @property path the path of the dependency project
   * @property isTestFixture Is the dependency being invoked via `testFixtures(project(...))`?
   * @property codeGeneratorBindingOrNull If it exists, this is the defined [CodeGeneratorBinding]
   */
  class CodeGeneratorProjectDependency(
    override val configurationName: ConfigurationName,
    override val path: ProjectPath,
    override val isTestFixture: Boolean,
    override val codeGeneratorBindingOrNull: CodeGeneratorBinding?
  ) : ProjectDependency(), MightHaveCodeGeneratorBinding

  /** @suppress */
  operator fun component1(): ConfigurationName = configurationName

  /** @suppress */
  operator fun component2(): ProjectPath = path

  /** @suppress */
  operator fun component3(): Boolean = isTestFixture

  /**
   * @return the most-downstream [SourceSetName] which contains declarations used by this dependency
   *   configuration. For a simple `implementation` configuration, this
   *   returns `main`. For a `debugImplementation`, it would return `debug`.
   */
  fun declaringSourceSetName(isAndroid: Boolean) = when {
    // <anyConfig>(testFixtures(___))
    isTestFixture -> {
      SourceSetName.TEST_FIXTURES
    }

    configurationName.toSourceSetName().isTestingOnly() -> {
      if (isAndroid) SourceSetName.DEBUG
      else SourceSetName.MAIN
    }

    // testFixturesApi(___)
    configurationName.toSourceSetName() == SourceSetName.TEST_FIXTURES -> {
      SourceSetName.MAIN
    }

    else -> {
      configurationName.toSourceSetName()
    }
  }

  /** Let's pretend this is a data class. */
  fun copy(
    configurationName: ConfigurationName = this.configurationName,
    path: ProjectPath = this.path,
    isTestFixture: Boolean = this.isTestFixture
  ): ProjectDependency {
    return when (this) {
      is RuntimeProjectDependency -> RuntimeProjectDependency(
        configurationName = configurationName,
        path = path,
        isTestFixture = isTestFixture
      )

      is CodeGeneratorProjectDependency -> CodeGeneratorProjectDependency(
        configurationName = configurationName,
        path = path,
        isTestFixture = isTestFixture,
        codeGeneratorBindingOrNull = codeGeneratorBindingOrNull
      )
    }
  }

  final override fun toString(): String {

    val declaration = if (isTestFixture) {
      "${configurationName.value}(testFixtures(project(path = \"${path.value}\")))"
    } else {
      "${configurationName.value}(project(path = \"${path.value}\"))"
    }

    return "${this::class.simpleName}( $declaration )"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ProjectDependency

    if (configurationName != other.configurationName) return false
    if (path != other.path) return false
    if (isTestFixture != other.isTestFixture) return false

    return true
  }

  override fun hashCode(): Int {
    var result = javaClass.hashCode()
    result = 31 * result + configurationName.hashCode()
    result = 31 * result + path.hashCode()
    result = 31 * result + isTestFixture.hashCode()
    return result
  }

  /**
   * Creates a [ProjectDependency] for given arguments, using [TypeSafeProjectPathResolver] and a
   * `List<CodeGeneratorBinding>` to look up a [CodeGeneratorBinding] in the event that the project
   * dependency in question is an annotation processor.
   */
  fun interface Factory {

    /** @return the [ProjectDependency] for this dependency declaration */
    fun create(
      configurationName: ConfigurationName,
      path: ProjectPath,
      isTestFixture: Boolean
    ): ProjectDependency
  }
}
