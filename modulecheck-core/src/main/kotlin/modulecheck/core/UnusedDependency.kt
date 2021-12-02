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

package modulecheck.core

import modulecheck.api.Deletable
import modulecheck.project.ConfigurationName
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.project.McProject
import java.io.File

data class UnusedDependency(
  override val dependentPath: String,
  override val buildFile: File,
  override val dependencyProject: McProject,
  override val dependencyIdentifier: String,
  override val configurationName: ConfigurationName,
  val isTestFixture: Boolean
) : DependencyFinding("unusedDependency"),
  Deletable {

  override val message: String
    get() = "The declared dependency is not used in this module."

  fun cpd() = ConfiguredProjectDependency(
    configurationName = configurationName,
    project = dependencyProject,
    isTestFixture = isTestFixture
  )

  override fun toString(): String {
    return "UnusedDependency(\n" +
      "\tdependentPath='$dependentPath', \n" +
      "\tbuildFile=$buildFile, \n" +
      "\tdependencyProject=$dependencyProject, \n" +
      "\tdependencyIdentifier='$dependencyIdentifier', \n" +
      "\tconfigurationName=$configurationName\n" +
      ")"
  }

  override fun fromStringOrEmpty(): String = ""
}
