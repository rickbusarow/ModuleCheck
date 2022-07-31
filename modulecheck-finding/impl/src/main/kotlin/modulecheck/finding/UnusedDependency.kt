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

package modulecheck.finding

import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.ConfiguredDependency
import modulecheck.model.dependency.Identifier
import modulecheck.project.McProject

data class UnusedDependency(
  val dependentProject: McProject,
  val dependency: ConfiguredDependency,
  val dependencyIdentifier: Identifier,
  val configurationName: ConfigurationName
) {

  fun toFinding(findingName: FindingName): UnusedDependencyFinding = UnusedDependencyFinding(
    findingName = findingName,
    dependentProject = dependentProject,
    oldDependency = dependency,
    dependencyIdentifier = dependencyIdentifier.name,
    configurationName = configurationName
  )
}

data class UnusedDependencyFinding(
  override val findingName: FindingName,
  override val dependentProject: McProject,
  override val oldDependency: ConfiguredDependency,
  override val dependencyIdentifier: String,
  override val configurationName: ConfigurationName
) : AbstractProjectDependencyFinding(),
  RemovesDependency,
  Deletable {

  override val dependency get() = oldDependency

  override val message: String
    get() = when {
      dependency.isTestFixture -> {
        "The declared dependency " +
          "`${configurationName.value}(testFixtures(\"${dependency.identifier}\"))` " +
          "is not used in this module."
      }

      else -> {
        "The declared dependency `${configurationName.value}(\"${dependency.identifier}\")` " +
          "is not used in this module."
      }
    }

  override fun toString(): String {
    return """UnusedDependency(
    	dependentPath='$dependentPath',
    	buildFile=$buildFile,
    	dependency=$dependency,
    	dependencyIdentifier='$dependencyIdentifier',
    	configurationName=$configurationName
    )
    """.trimIndent()
  }

  override fun fromStringOrEmpty(): String = ""
}
