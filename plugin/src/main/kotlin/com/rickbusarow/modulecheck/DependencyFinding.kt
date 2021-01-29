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

import org.gradle.api.Project

interface Finding {
  val dependentProject: Project

  fun logString(): String
  fun position(): Position?
}

interface Fixable {

  val problemName: String

  fun fix()

  fun label() = "$FIX_LABEL -- $problemName"

  companion object {

    const val FIX_LABEL = "  // ModuleCheck finding"
    const val INLINE_COMMENT = "// "
  }
}

data class UnusedDependency(
  override val dependentProject: Project,
  override val dependencyProject: Project,
  override val dependencyPath: String,
  override val config: Config
) : DependencyFinding("unused") {
  fun cpp() = CPP(config, dependencyProject)
}

data class RedundantDependency(
  override val dependentProject: Project,
  override val dependencyProject: Project,
  override val dependencyPath: String,
  override val config: Config,
  val from: List<Project>
) : DependencyFinding("redundant") {
  override fun logString(): String = super.logString() + " from: ${from.joinToString { it.path }}"
}

abstract class DependencyFinding(override val problemName: String) : Fixable, Finding {

  abstract val dependencyProject: Project
  abstract val dependencyPath: String
  abstract val config: Config
  override fun position(): Position? {
    return MCP.from(dependencyProject)
      .positionIn(dependentProject, config)
  }

  override fun logString(): String {
    val pos = position()?.logString() ?: ""

    return "${dependentProject.buildFile.path}: $pos$dependencyPath"
  }

  override fun fix() {
    val text = dependentProject.buildFile.readText()

    position()?.let { position ->

      val row = position.row - 1

      val lines = text.lines().toMutableList()

      if (row > 0) {
        lines[row] = Fixable.INLINE_COMMENT + lines[row] + label()

        val newText = lines.joinToString("\n")

        dependentProject.buildFile.writeText(newText)
      }
    }
  }
}
