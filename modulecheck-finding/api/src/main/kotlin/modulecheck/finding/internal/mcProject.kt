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

package modulecheck.finding.internal

import modulecheck.finding.Finding.Position
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.ExternalDependencyDeclaration
import modulecheck.parsing.gradle.ModuleDependencyDeclaration
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.ExternalDependency
import modulecheck.project.McProject

suspend fun ConfiguredProjectDependency.statementOrNullIn(
  dependentProject: McProject
): ModuleDependencyDeclaration? {
  return dependentProject.buildFileParser
    .dependenciesBlocks()
    .firstNotNullOfOrNull { block ->
      block.getOrEmpty(path, configurationName, isTestFixture)
        .takeIf { it.isNotEmpty() }
    }
    ?.firstOrNull()
}

suspend fun ExternalDependency.statementOrNullIn(
  dependentProject: McProject,
  configuration: ConfigurationName
): ExternalDependencyDeclaration? {
  return dependentProject.buildFileParser
    .dependenciesBlocks()
    .firstNotNullOfOrNull { block ->
      block.getOrEmpty(coords, configuration)
        .takeIf { it.isNotEmpty() }
    }
    ?.firstOrNull()
}

suspend fun ConfiguredProjectDependency.positionIn(
  dependentProject: McProject
): Position? {

  val statement = statementOrNullIn(
    dependentProject = dependentProject
  ) ?: return null

  return dependentProject.buildFile.readText()
    .positionOfStatement(statement.statementWithSurroundingText)
}
