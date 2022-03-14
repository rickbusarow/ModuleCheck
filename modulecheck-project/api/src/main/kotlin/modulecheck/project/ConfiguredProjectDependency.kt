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
import modulecheck.parsing.gradle.SourceSetName

data class ConfiguredProjectDependency(
  override val configurationName: ConfigurationName,
  val project: McProject,
  val isTestFixture: Boolean
) : ConfiguredDependency {

  val path = project.path

  override val name = project.path

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
      "${configurationName.value}(testFixtures(project(path = \"$path\")))"
    } else {
      "${configurationName.value}(project(path = \"$path\"))"
    }

    return "ConfiguredProjectDependency( $declaration )"
  }
}

data class TransitiveProjectDependency(
  val source: ConfiguredProjectDependency,
  val contributed: ConfiguredProjectDependency
) {
  override fun toString(): String {
    return """TransitiveProjectDependency(
      |       source=$source
      |  contributed=$contributed
      |)
    """.trimMargin()
  }
}
