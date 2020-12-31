package com.rickbusarow.modulecheck.parser

import com.rickbusarow.modulecheck.Config
import com.rickbusarow.modulecheck.Finding
import com.rickbusarow.modulecheck.Fixable
import com.rickbusarow.modulecheck.MCP
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency

object KaptParser {

  fun parseLazy(mcp: MCP): Lazy<MCP.ParsedKapt<KaptProcessor>> = lazy {
    parse(mcp)
  }

  fun parse(mcp: MCP): MCP.ParsedKapt<KaptProcessor> {
    val grouped = mcp.project
      .configurations
      .groupBy { it.name }
      .mapValues { (_, configurations) ->
        configurations.flatMap { config ->
          config
            .dependencies
            .map { dep: Dependency ->

              val comb = dep.group + ":" + dep.name

              KaptProcessor(comb)
            }
        }.toSet()
      }

    return MCP.ParsedKapt(
      grouped.getOrDefault("kaptAndroidTest", setOf()),
      grouped.getOrDefault("kapt", setOf()),
      grouped.getOrDefault("kaptTest", setOf())
    )
  }
}

sealed class UnusedKapt : Finding, Fixable {
}

data class UnusedKaptPlugin(
  override val dependentProject: Project
) : UnusedKapt() {

  fun position(): MCP.Position {
    return MCP.Position(0, 0)
  }

  fun logString(): String {
    val pos = if (position().row == 0 || position().column == 0) {
      ""
    } else {
      "(${position().row}, ${position().column}): "
    }

    return "${dependentProject.buildFile.path}: $pos"
  }

  override fun fix() {
    val text = dependentProject.buildFile.readText()

    val row = position().row - 1

    val lines = text.lines().toMutableList()

    if (row > 0) {
      lines[row] = "//" + lines[row]

      val newText = lines.joinToString("\n")

      dependentProject.buildFile.writeText(newText)
    }
  }
}


data class UnusedKaptProcessor(
  override val dependentProject: Project,
    val dependencyPath: String,
  val config: Config
) : UnusedKapt() {

  fun position(): MCP.Position {
    return MCP.Position(0, 0)
  }

  fun logString(): String {
    val pos = if (position().row == 0 || position().column == 0) {
      ""
    } else {
      "(${position().row}, ${position().column}): "
    }

    return "${dependentProject.buildFile.path}: $pos$dependencyPath"
  }

  override fun fix() {
    val text = dependentProject.buildFile.readText()

    val row = position().row - 1

    val lines = text.lines().toMutableList()

    if (row > 0) {
      lines[row] = "//" + lines[row]

      val newText = lines.joinToString("\n")

      dependentProject.buildFile.writeText(newText)
    }
  }
}
