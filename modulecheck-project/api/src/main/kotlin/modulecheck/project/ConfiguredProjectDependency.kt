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

package modulecheck.project

import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.ProjectPath.StringProjectPath
import modulecheck.parsing.gradle.SourceSetName

data class ConfiguredProjectDependency(
  override val configurationName: ConfigurationName,
  val project: McProject,
  val isTestFixture: Boolean
) : ConfiguredDependency {

  val path = project.path

  override val name = project.path.value

  fun declaringSourceSetName() = when {
    isTestFixture -> {
      SourceSetName.TEST_FIXTURES
    }
    configurationName.toSourceSetName().isTestingOnly() -> {
      SourceSetName.MAIN
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

    return "ConfiguredProjectDependency( $declaration )"
  }
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
  val dependentProject: McProject,
  val configuredProjectDependency: ConfiguredProjectDependency
)

data class SourceSetDependency(
  val sourceSetName: SourceSetName,
  val path: StringProjectPath,
  val isTestFixture: Boolean
)

fun ConfiguredProjectDependency.toSourceSetDependency(
  sourceSetName: SourceSetName = configurationName.toSourceSetName(),
  path: StringProjectPath = this@toSourceSetDependency.path,
  isTestFixture: Boolean = this@toSourceSetDependency.isTestFixture
) = SourceSetDependency(
  sourceSetName = sourceSetName,
  path = path,
  isTestFixture = isTestFixture
)
