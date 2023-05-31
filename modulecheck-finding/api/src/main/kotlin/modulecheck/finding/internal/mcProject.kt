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

package modulecheck.finding.internal

import modulecheck.finding.Finding.Position
import modulecheck.model.dependency.ConfiguredDependency
import modulecheck.model.dependency.ExternalDependency
import modulecheck.model.dependency.ProjectDependency
import modulecheck.parsing.gradle.dsl.DependencyDeclaration
import modulecheck.project.McProject

suspend fun ConfiguredDependency.statementOrNullIn(
  dependentProject: McProject
): DependencyDeclaration? {
  return dependentProject.buildFileParser
    .dependenciesBlocks()
    .firstNotNullOfOrNull { block ->

      when (this) {
        is ExternalDependency -> block.getOrEmpty(mavenCoordinates, configurationName)
          .takeIf { it.isNotEmpty() }

        is ProjectDependency -> block.getOrEmpty(projectPath, configurationName, isTestFixture)
          .takeIf { it.isNotEmpty() }
      }
    }
    ?.firstOrNull()
}

suspend fun ConfiguredDependency.positionIn(dependentProject: McProject): Position? {

  val statement = statementOrNullIn(dependentProject) ?: return null

  return dependentProject.buildFile.readText()
    .positionOfStatement(statement.statementWithSurroundingText)
}
