package com.rickbusarow.modulecheck.testing

import java.io.File
import java.nio.file.Path

class ProjectSpec private constructor(
  private val path: String,
  private val subprojects: MutableList<ProjectSpec>,
  private val projectSettingsSpec: ProjectSettingsSpec?,
  private val projectBuildSpec: ProjectBuildSpec?,
  private val projectSrcSpecs: MutableList<ProjectSrcSpec>
) {

  fun writeIn(path: Path) {
    projectSettingsSpec?.writeIn(path)
    projectBuildSpec?.writeIn(path)
    subprojects.forEach { it.writeIn(Path.of(path.toString(), it.path)) }
    projectSrcSpecs.forEach { it.writeIn(path) }
  }

  class Builder(val filePath: String) {

    private val subprojects = mutableListOf<ProjectSpec>()
    private var projectSettingsSpec: ProjectSettingsSpec? = null
    private var projectBuildSpec: ProjectBuildSpec? = null
    private val projectSrcSpecs = mutableListOf<ProjectSrcSpec>()

    fun addSubproject(projectSpec: ProjectSpec) = apply {
      subprojects.add(projectSpec)
    }

    fun addSettingsSpec(projectSettingsSpec: ProjectSettingsSpec) = apply {
      this.projectSettingsSpec = projectSettingsSpec
    }

    fun addBuildSpec(projectBuildSpec: ProjectBuildSpec) = apply {
      this.projectBuildSpec = projectBuildSpec
    }

    fun addSrcSpec(projectSrcSpec: ProjectSrcSpec) = apply {
      this.projectSrcSpecs.add(projectSrcSpec)
    }

    fun build(): ProjectSpec =
      ProjectSpec(filePath, subprojects, projectSettingsSpec, projectBuildSpec, projectSrcSpecs)
  }
}

fun Path.newFile(fileName: String): File = File(this.toFile(), fileName)
fun File.newFile(fileName: String): File = File(this, fileName)
