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

package modulecheck.core.overshot

import modulecheck.api.Config
import modulecheck.api.Finding.Position
import modulecheck.api.Project2
import modulecheck.core.DependencyFinding
import modulecheck.core.MCP
import modulecheck.core.kotlinBuildFileOrNull
import modulecheck.psi.DslBlockVisitor

data class OverShotDependencyFinding(
  override val dependentProject: Project2,
  override val dependencyProject: Project2,
  val dependencyPath: String,
  override val config: Config,
  val from: MCP?
) : DependencyFinding("over-shot") {

  override val dependencyIdentifier = dependencyPath + " from: ${from?.path}"

  override fun positionOrNull(): Position? {
    return from?.positionIn(dependentProject, config)
  }

  override fun fix(): Boolean {
    val visitor = DslBlockVisitor("dependencies")

    val fromPath = from?.path ?: return false

    val kotlinBuildFile = kotlinBuildFileOrNull() ?: return false

    val result = visitor.parse(kotlinBuildFile) ?: return false

    val match = result.elements.firstOrNull {
      it.psiElement.text.contains(fromPath)
    }
      ?.toString() ?: return false

    val newDeclaration = match.replace(fromPath, dependencyPath)

    // This won't match without .trimStart()
    val newDependencies = result.blockText.replace(
      oldValue = match.trimStart(),
      newValue = (newDeclaration + "\n" + match).trimStart()
    )

    val text = dependentProject.buildFile.readText()

    val newText = text.replace(result.blockText, newDependencies)

    dependentProject.buildFile.writeText(newText)

    return true
  }
}
