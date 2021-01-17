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

package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.internal.asKtFile
import com.rickbusarow.modulecheck.parser.DslBlockParser
import org.gradle.api.Project

data class OverShotDependency(
  override val dependentProject: Project,
  override val dependencyProject: Project,
  override val dependencyPath: String,
  override val config: Config,
  val from: MCP?
) : DependencyFinding("over-shot") {

  override fun position(): Position? {
    return from?.positionIn(dependentProject.project, config)
  }

  override fun logString(): String = super.logString() + " from: ${from?.path}"

  override fun fix() {
    val parser = DslBlockParser("dependencies")

    val fromPath = from?.path ?: return

    val result = parser.parse(dependentProject.buildFile.asKtFile()) ?: return

    val match = result.elements.firstOrNull {
      it.psiElement.text.contains(fromPath)
    }
      ?.toString() ?: return

    val newDeclaration = match.replace(fromPath, dependencyPath)

    // This won't match without .trimStart()
    val newDependencies = result.blockText.replace(
      oldValue = match.trimStart(),
      newValue = (newDeclaration + "\n" + match).trimStart()
    )

    val text = dependentProject.buildFile.readText()

    val newText = text.replace(result.blockText, newDependencies)

    dependentProject.buildFile.writeText(newText)
  }
}
