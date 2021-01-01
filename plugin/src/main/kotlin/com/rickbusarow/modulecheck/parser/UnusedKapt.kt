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

package com.rickbusarow.modulecheck.parser

import com.rickbusarow.modulecheck.*
import com.rickbusarow.modulecheck.rule.KAPT_PLUGIN_FUN
import com.rickbusarow.modulecheck.rule.KAPT_PLUGIN_ID
import org.gradle.api.Project

sealed class UnusedKapt : Finding, Fixable {

  override fun fix() {
    val text = dependentProject.buildFile.readText()

    position()?.let { position ->

      val row = position.row - 1

      val lines = text.lines().toMutableList()

      if (row > 0) {
        lines[row] = "//" + lines[row]

        val newText = lines.joinToString("\n")

        dependentProject.buildFile.writeText(newText)
      }
    }
  }
}

data class UnusedKaptPlugin(
  override val dependentProject: Project
) : UnusedKapt() {

  override fun position(): Position? {
    val text = dependentProject
      .buildFile
      .readText()

    val lines = text.lines()

    val row = lines
      .indexOfFirst { line ->
        line.contains("id(\"$KAPT_PLUGIN_ID\")") ||
          line.contains(KAPT_PLUGIN_FUN) ||
          line.contains("plugin = \"$KAPT_PLUGIN_ID\")")
      }

    if (row < 0) return null

    val col = lines[row]
      .indexOfFirst { it != ' ' }

    return Position(row + 1, col + 1)
  }

  override fun logString(): String {
    val pos = position()?.let {
      "(${it.row}, ${it.column}): "
    } ?: ""

    return "unused kapt plugin: ${dependentProject.buildFile.path}: $pos$KAPT_PLUGIN_ID"
  }
}

data class UnusedKaptProcessor(
  override val dependentProject: Project,
  val dependencyPath: String,
  val config: Config
) : UnusedKapt() {

  override fun position(): Position? {
    val fixedPath = dependencyPath.split(".")
      .drop(1)
      .joinToString(":", ":")

    return dependentProject
      .buildFile
      .readText()
      .lines()
      .positionOf(dependencyPath, config) ?: dependentProject
      .buildFile
      .readText()
      .lines()
      .positionOf(fixedPath, config)
  }

  override fun logString(): String {
    val pos = position()?.let {
      "(${it.row}, ${it.column}): "
    } ?: ""

    return "unused ${config.name} dependency: ${dependentProject.buildFile.path}: $pos$dependencyPath"
  }
}
