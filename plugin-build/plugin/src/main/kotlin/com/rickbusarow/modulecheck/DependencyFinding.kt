package com.rickbusarow.modulecheck

import org.gradle.api.Project

sealed class DependencyFinding(val problemName: String) {
  abstract val dependentProject: Project
  abstract val position: ModuleCheckProject.Position
  abstract val dependencyPath: String
  abstract val configurationName: String

  data class UnusedDependency(
    override val dependentProject: Project,
    override val position: ModuleCheckProject.Position,
    override val dependencyPath: String,
    override val configurationName: String
  ) : DependencyFinding("unused")

  data class OverShotDependency(
    override val dependentProject: Project,
    override val dependencyPath: String,
    override val configurationName: String,
    override val position: ModuleCheckProject.Position,
    val from: List<ModuleCheckProject>
  ) : DependencyFinding("over-shot") {

    override fun logString(): String =
      super.logString() + " from: ${from.joinToString { it.path }}"

    override fun fix() {

      val text = dependentProject.buildFile.readText()

      val row = position.row - 1

      val lines = text.lines().toMutableList()

      if (row > 0 && from.isNotEmpty()) {

        val existingPath = from.first().path

        val existingLine = lines[row]

        lines[row] = existingLine + "\n" + existingLine.replace(existingPath, dependencyPath)

        val newText = lines.joinToString("\n")

        dependentProject.buildFile.writeText(newText)
      }
    }
  }

  data class RedundantDependency(
    override val dependentProject: Project,
    override val position: ModuleCheckProject.Position,
    override val dependencyPath: String,
    override val configurationName: String,
    val from: List<Project>
  ) : DependencyFinding("redundant") {
    override fun logString(): String = super.logString() + " from: ${from.joinToString { it.path }}"
  }

  open fun logString(): String {

    val pos =
      if (position.row == 0 || position.column == 0) ""
      else "(${position.row}, ${position.column}): "

    return "${dependentProject.buildFile.path}: $pos${dependencyPath}"
  }

  open fun fix() {

    val text = dependentProject.buildFile.readText()

    val row = position.row - 1

    val lines = text.lines().toMutableList()

    if (row > 0) {

      lines[row] = "//" + lines[row]

      val newText = lines.joinToString("\n")

      dependentProject.buildFile.writeText(newText)
    }
  }
}
