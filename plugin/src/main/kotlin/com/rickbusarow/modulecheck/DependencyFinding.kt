/*
 * Copyright (C) 2020 Rick Busarow
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

import com.rickbusarow.modulecheck.parser.PsiElementWithSurroundingText
import org.gradle.api.Project

interface Finding {

  val problemName: String
  val dependentProject: Project

  fun logString(): String {
    return "${dependentProject.buildFile.path}: ${positionString()} $problemName"
  }

  fun elementOrNull(): PsiElementWithSurroundingText? = null
  fun positionOrNull(): Position?
  fun positionString() = positionOrNull()?.logString() ?: ""
}

interface Fixable : Finding {

  val dependencyIdentifier: String

  override fun logString(): String {
    return "${dependentProject.buildFile.path}: ${positionString()} $problemName: $dependencyIdentifier"
  }

  fun fix() {
    val text = dependentProject.buildFile.readText()

    elementOrNull()
      ?.psiElement
      ?.let { element ->

        val newText = element.text
          .lines()
          .mapIndexed { index: Int, str: String ->
            if (index == 0) {
              INLINE_COMMENT + str + fixLabel()
            } else {
              INLINE_COMMENT + str
            }
          }
          .joinToString("\n")

        dependentProject.buildFile
          .writeText(text.replace(element.text, newText))
      }
  }

  fun fixLabel() = "  $FIX_LABEL [$problemName] "

  companion object {

    const val FIX_LABEL = "// ModuleCheck finding"
    const val INLINE_COMMENT = "// "
  }
}

abstract class DependencyFinding(override val problemName: String) : Fixable, Finding {

  abstract val dependencyProject: Project
  abstract val config: Config

  override fun elementOrNull(): PsiElementWithSurroundingText? {
    return MCP.from(dependencyProject)
      .psiElementIn(dependentProject, config)
  }

  override fun positionOrNull(): Position? {
    return MCP.from(dependencyProject)
      .positionIn(dependentProject, config)
  }
}
