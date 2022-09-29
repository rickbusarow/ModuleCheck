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

import modulecheck.model.dependency.SourceSetDependency.SourceSetExternalDependency
import modulecheck.model.dependency.SourceSetDependency.SourceSetProjectDependency
import modulecheck.model.sourceset.SourceSetName
import modulecheck.utils.lazy.unsafeLazy

sealed interface SourceSetDependency : HasIdentifier {

  val sourceSetName: SourceSetName
  val isTestFixture: Boolean

  data class SourceSetExternalDependency(
    override val sourceSetName: SourceSetName,
    val group: String?,
    val moduleName: String,
    val version: String?,
    override val isTestFixture: Boolean
  ) : SourceSetDependency, HasMavenCoordinates {
    override val mavenCoordinates: MavenCoordinates by unsafeLazy {
      MavenCoordinates(
        group = group,
        moduleName = moduleName,
        version = version
      )
    }
  }

  data class SourceSetProjectDependency(
    override val sourceSetName: SourceSetName,
    override val projectPath: ProjectPath,
    override val isTestFixture: Boolean
  ) : SourceSetDependency, HasProjectPath
}

fun ConfiguredDependency.toSourceSetDependency(
  sourceSetName: SourceSetName = configurationName.toSourceSetName()
): SourceSetDependency = when (this) {
  is ExternalDependency -> SourceSetExternalDependency(
    sourceSetName = sourceSetName,
    group = group,
    moduleName = moduleName,
    version = version,
    isTestFixture = isTestFixture
  )

  is ProjectDependency -> SourceSetProjectDependency(
    sourceSetName = sourceSetName,
    projectPath = projectPath,
    isTestFixture = isTestFixture
  )
}
