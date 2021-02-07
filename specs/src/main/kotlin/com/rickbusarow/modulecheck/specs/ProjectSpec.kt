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

package com.rickbusarow.modulecheck.specs

import java.io.File
import java.nio.file.Path

@Suppress("MemberVisibilityCanBePrivate")
public class ProjectSpec private constructor(
  public val gradlePath: String,
  private val subprojects: List<ProjectSpec>,
  private val projectSettingsSpec: ProjectSettingsSpec?,
  private val projectBuildSpec: ProjectBuildSpec?,
  private val projectSrcSpecs: List<ProjectSrcSpec>
) {

  public fun toBuilder(): Builder = Builder(gradlePath).apply {
    applyEach(subprojects) { addSubproject(it) }
    applyEach(projectSrcSpecs) { addSrcSpec(it) }
    if (this@ProjectSpec.projectSettingsSpec != null) {
      addSettingsSpec(this@ProjectSpec.projectSettingsSpec)
    }
    if (this@ProjectSpec.projectBuildSpec != null) {
      addBuildSpec(this@ProjectSpec.projectBuildSpec)
    }
  }

  public fun writeIn(path: Path) {
    projectSettingsSpec?.writeIn(path)
    projectBuildSpec?.writeIn(path)
    subprojects.forEach { it.writeIn(Path.of(path.toString(), it.gradlePath)) }
    projectSrcSpecs.forEach { it.writeIn(path) }
  }

  @Suppress("TooManyFunctions")
  public class Builder internal constructor(public val filePath: String) {

    private val _subprojects: MutableList<ProjectSpec> = mutableListOf<ProjectSpec>()
    private val _projectSrcSpecs = mutableListOf<ProjectSrcSpec>()

    public val subprojects: List<ProjectSpec>
      get() = _subprojects
    public val projectSrcSpecs: List<ProjectSrcSpec>
      get() = _projectSrcSpecs

    public var projectSettingsSpec: ProjectSettingsSpec? = null
      private set
    public var projectBuildSpec: ProjectBuildSpec? = null
      private set

    public fun addSubproject(projectSpec: ProjectSpec): Builder = apply {
      _subprojects.add(projectSpec)
    }

    public fun addSubprojects(vararg projectSpecs: ProjectSpec): Builder = apply {
      _subprojects.addAll(projectSpecs)
    }

    public fun addSettingsSpec(projectSettingsSpec: ProjectSettingsSpec): Builder = apply {
      this.projectSettingsSpec = projectSettingsSpec
    }

    public fun addBuildSpec(projectBuildSpec: ProjectBuildSpec): Builder = apply {
      this.projectBuildSpec = projectBuildSpec
    }

    public fun addSrcSpec(projectSrcSpec: ProjectSrcSpec): Builder = apply {
      this._projectSrcSpecs.add(projectSrcSpec)
    }

    public fun build(): ProjectSpec =
      ProjectSpec(filePath, subprojects, projectSettingsSpec, projectBuildSpec, projectSrcSpecs)
  }

  public companion object {

    public fun builder(path: Path): Builder = Builder(path.toString())

    public fun builder(path: String): Builder = Builder(path)
  }
}

public fun Path.newFile(fileName: String): File = File(this.toFile(), fileName)
public fun File.newFile(fileName: String): File = File(this, fileName)
