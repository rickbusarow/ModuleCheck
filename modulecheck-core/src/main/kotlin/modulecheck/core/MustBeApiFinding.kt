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

import modulecheck.api.ConfigurationName
import modulecheck.api.ConfiguredProjectDependency
import modulecheck.api.Project2
import modulecheck.core.internal.statementOrNullIn
import java.io.File

data class MustBeApiFinding(
  override val dependentPath: String,
  override val buildFile: File,
  override val dependencyProject: Project2,
  override val configurationName: ConfigurationName,
  val source: ConfiguredProjectDependency?
) : DependencyFinding("mustBeApi") {

  override val dependencyIdentifier = dependencyProject.path + " from: ${source?.project?.path}"

  override val statementTextOrNull: String? by lazy {
    super.statementTextOrNull
      ?: source?.project
        ?.statementOrNullIn(buildFile, configurationName)
  }

  override fun fix(): Boolean = synchronized(buildFile) {

    val statement = statementTextOrNull ?: return false

    val newText = statement.replace(configurationName.value, "api")

    val buildFileText = buildFile.readText()

    buildFile.writeText(buildFileText.replace(statement, newText))

    return true
  }

  override fun toString(): String {
    return """MustBeApiFinding(
      |   dependentPath='$dependentPath',
      |   buildFile=$buildFile,
      |   dependencyProject=$dependencyProject,
      |   configurationName=$configurationName,
      |   source=$source,
      |   dependencyIdentifier='$dependencyIdentifier'
      |)""".trimMargin()
  }
}
