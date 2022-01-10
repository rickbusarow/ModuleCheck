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

package modulecheck.core.anvil

import modulecheck.api.finding.Finding
import modulecheck.api.finding.Finding.Position
import modulecheck.api.finding.Fixable
import modulecheck.core.internal.positionOf
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.DependencyDeclaration
import modulecheck.project.McProject
import modulecheck.utils.existsOrNull
import java.io.File

data class CouldUseAnvilFinding(
  override val dependentProject: McProject,
  override val buildFile: File
) : Finding, Fixable {

  override val dependentPath = dependentProject.path

  override val message: String
    get() = "Dagger's compiler could be replaced with Anvil's factory generation for faster builds."

  override val dependencyIdentifier = "com.google.dagger:dagger-compiler"
  override val findingName = "useAnvilFactories"

  override val declarationOrNull: DependencyDeclaration?
    get() = null

  override val positionOrNull: Position? by lazy {

    val statement = statementTextOrNull ?: return@lazy null

    buildFile
      .existsOrNull()
      ?.readText()
      ?.lines()
      ?.positionOf(statement, ConfigurationName.kapt)
  }

  override val statementTextOrNull: String? get() = null
}
