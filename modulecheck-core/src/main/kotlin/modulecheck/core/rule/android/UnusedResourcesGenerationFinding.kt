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

package modulecheck.core.rule.android

import modulecheck.api.finding.Finding
import modulecheck.api.finding.Finding.Position
import modulecheck.api.finding.Fixable
import modulecheck.core.internal.positionOfStatement
import modulecheck.core.kotlinBuildFileOrNull
import modulecheck.parsing.psi.AndroidBuildFeaturesVisitor
import modulecheck.project.McProject
import java.io.File

data class UnusedResourcesGenerationFinding(
  override val dependentProject: McProject,
  override val dependentPath: String,
  override val buildFile: File
) : Finding, Fixable {

  override val message: String
    get() = "`androidResources` generation is enabled, but no resources are defined in this module."

  override val findingName = "disableAndroidResources"

  override val dependencyIdentifier = ""

  override val statementTextOrNull: String? by lazy {
    val buildFile = kotlinBuildFileOrNull() ?: return@lazy null

    AndroidBuildFeaturesVisitor().find(buildFile, "androidResources")
      ?.statementText
  }

  override val positionOrNull: Position? by lazy {
    val statement = statementTextOrNull ?: return@lazy null

    val fileText = buildFile.readText()

    fileText.positionOfStatement(statement)
  }

  override fun fix(): Boolean = synchronized(buildFile) {
    val ktFile = kotlinBuildFileOrNull() ?: return false

    val oldBlock = statementTextOrNull ?: return false

    val newBlock = oldBlock.replace("true", "false")

    val oldText = ktFile.text

    buildFile.writeText(oldText.replace(oldBlock, newBlock))

    return true
  }
}
