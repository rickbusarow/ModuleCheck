/*
 * Copyright (C) 2021 Rick Busarow
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

package modulecheck.core.internal

import modulecheck.api.Finding.Position
import modulecheck.api.positionOfStatement
import modulecheck.core.parse
import modulecheck.parsing.ConfigurationName
import modulecheck.parsing.DependencyBlockParser
import modulecheck.parsing.ModuleDependencyDeclaration
import modulecheck.parsing.McProject
import java.io.File

fun McProject.statementOrNullIn(
  dependentBuildFile: File,
  configuration: ConfigurationName
): ModuleDependencyDeclaration? {
  return DependencyBlockParser
    .parse(dependentBuildFile)
    .firstNotNullOfOrNull { block ->
      block.getOrEmpty(path, configuration)
        .takeIf { it.isNotEmpty() }
    }
    ?.firstOrNull()
}

fun McProject.positionIn(
  dependentBuildFile: File,
  configuration: ConfigurationName
): Position? {

  val statement = statementOrNullIn(dependentBuildFile, configuration) ?: return null

  return dependentBuildFile.readText()
    .positionOfStatement(statement.statementWithSurroundingText)
}
