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

package modulecheck.parsing.gradle.model

import modulecheck.config.CodeGeneratorBinding
import modulecheck.config.HasCodeGeneratorBinding
import modulecheck.parsing.gradle.model.ProjectPath.StringProjectPath

sealed class ConfiguredProjectDependency : ConfiguredDependency, HasPath {

  abstract val isTestFixture: Boolean
  override val name = path.value

  /**
   * @return the most-downstream [SourceSetName] which contains declarations used by this dependency
   *   configuration. For a simple `implementation` configuration, this
   *   returns `main`. For a `debugImplementation`, it would return `debug`.
   */
  fun declaringSourceSetName(isAndroid: Boolean) = when {
    isTestFixture -> {
      SourceSetName.TEST_FIXTURES
    }

    configurationName.toSourceSetName().isTestingOnly() -> {
      if (isAndroid) SourceSetName.DEBUG
      else SourceSetName.MAIN
    }

    else -> {
      configurationName.toSourceSetName()
    }
  }

  override fun toString(): String {

    val declaration = if (isTestFixture) {
      "${configurationName.value}(testFixtures(project(path = \"${path.value}\")))"
    } else {
      "${configurationName.value}(project(path = \"${path.value}\"))"
    }

    return "${this::class.simpleName}( $declaration )"
  }

  fun copy(
    configurationName: ConfigurationName = this.configurationName,
    path: ProjectPath = this.path,
    isTestFixture: Boolean = this.isTestFixture
  ): ConfiguredProjectDependency {
    return when (this) {
      is ConfiguredJvmProjectDependency -> copy(
        configurationName = configurationName,
        path = path,
        isTestFixture = isTestFixture
      )

      is ConfiguredAndroidProjectDependency -> copy(
        configurationName = configurationName,
        path = path,
        isTestFixture = isTestFixture
      )

      is ConfiguredCodeGeneratorProjectDependency -> copy(
        configurationName = configurationName,
        path = path,
        isTestFixture = isTestFixture,
        codeGeneratorBinding = codeGeneratorBinding
      )
    }
  }

  data class ConfiguredJvmProjectDependency(
    override val configurationName: ConfigurationName,
    override val path: ProjectPath,
    override val isTestFixture: Boolean
  ) : ConfiguredProjectDependency()

  data class ConfiguredAndroidProjectDependency(
    override val configurationName: ConfigurationName,
    override val path: ProjectPath,
    override val isTestFixture: Boolean
  ) : ConfiguredProjectDependency()

  data class ConfiguredCodeGeneratorProjectDependency(
    override val configurationName: ConfigurationName,
    override val path: ProjectPath,
    override val isTestFixture: Boolean,
    override val codeGeneratorBinding: CodeGeneratorBinding
  ) : ConfiguredProjectDependency(), HasCodeGeneratorBinding
}

data class TransitiveProjectDependency(
  val source: ConfiguredProjectDependency,
  val contributed: ConfiguredProjectDependency
) {

  fun withContributedConfiguration(
    configurationName: ConfigurationName = source.configurationName
  ): TransitiveProjectDependency {
    val newContributed = contributed.copy(configurationName = configurationName)
    return copy(contributed = newContributed)
  }

  override fun toString(): String {
    return """TransitiveProjectDependency(
      |       source=$source
      |  contributed=$contributed
      |)
    """.trimMargin()
  }
}

data class DownstreamDependency(
  val dependentProjectPath: StringProjectPath,
  val configuredProjectDependency: ConfiguredProjectDependency
)

data class SourceSetDependency(
  val sourceSetName: SourceSetName,
  override val path: ProjectPath,
  val isTestFixture: Boolean
) : HasPath

fun ConfiguredProjectDependency.toSourceSetDependency(
  sourceSetName: SourceSetName = configurationName.toSourceSetName(),
  path: ProjectPath = this@toSourceSetDependency.path,
  isTestFixture: Boolean = this@toSourceSetDependency.isTestFixture
) = SourceSetDependency(
  sourceSetName = sourceSetName,
  path = path,
  isTestFixture = isTestFixture
)
