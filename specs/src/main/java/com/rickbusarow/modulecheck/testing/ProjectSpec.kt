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

package com.rickbusarow.modulecheck.testing

import java.io.File
import java.nio.file.Path

class ProjectSpec private constructor(
  val gradlePath: String,
  private val subprojects: MutableList<ProjectSpec>,
  private val projectSettingsSpec: ProjectSettingsSpec?,
  private val projectBuildSpec: ProjectBuildSpec?,
  private val projectSrcSpecs: MutableList<ProjectSrcSpec>
) {

  fun writeIn(path: Path) {
    projectSettingsSpec?.writeIn(path)
    projectBuildSpec?.writeIn(path)
    subprojects.forEach { it.writeIn(Path.of(path.toString(), it.gradlePath)) }
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
