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

public data class ProjectSpec(
  public var gradlePath: String,
  public var projectSettingsSpec: ProjectSettingsSpec?,
  public var projectBuildSpec: ProjectBuildSpec?,
  public val subprojects: MutableList<ProjectSpec>,
  public val projectSrcSpecs: MutableList<ProjectSrcSpec>
) {

  public fun toBuilder(): ProjectSpecBuilder = ProjectSpecBuilder(
    gradlePath = gradlePath,
    projectSettingsSpec = projectSettingsSpec,
    projectBuildSpec = projectBuildSpec,
    subprojects = subprojects,
    projectSrcSpecs = projectSrcSpecs
  )

  public inline fun edit(
    init: ProjectSpecBuilder.() -> Unit
  ): ProjectSpec = toBuilder().apply { init() }.build()

  public fun writeIn(path: Path) {
    projectSettingsSpec?.writeIn(path)
    projectBuildSpec?.writeIn(path)
    subprojects.forEach { it.writeIn(Path.of(path.toString(), it.gradlePath)) }
    projectSrcSpecs.forEach { it.writeIn(path) }
  }

  public companion object {

    public operator fun invoke(
      gradlePath: String,
      init: ProjectSpecBuilder.() -> Unit
    ): ProjectSpec = ProjectSpecBuilder(gradlePath = gradlePath, init = init).build()

    public fun builder(
      gradlePath: String
    ): ProjectSpecBuilder = ProjectSpecBuilder(gradlePath = gradlePath)
  }
}

public class ProjectSpecBuilder(
  public var gradlePath: String,
  public var projectSettingsSpec: ProjectSettingsSpec? = null,
  public var projectBuildSpec: ProjectBuildSpec? = null,
  public val subprojects: MutableList<ProjectSpec> = mutableListOf(),
  public val projectSrcSpecs: MutableList<ProjectSrcSpec> = mutableListOf(),
  init: ProjectSpecBuilder.() -> Unit = {}
) : Builder<ProjectSpec> {

  init {
    init()
  }

  public fun addSubproject(projectSpec: ProjectSpec) {
    subprojects.add(projectSpec)
  }

  public fun addSubprojects(vararg projectSpecs: ProjectSpec) {
    subprojects.addAll(projectSpecs)
  }

  public fun addSettingsSpec(projectSettingsSpec: ProjectSettingsSpec) {
    this.projectSettingsSpec = projectSettingsSpec
  }

  public fun addBuildSpec(projectBuildSpec: ProjectBuildSpec) {
    this.projectBuildSpec = projectBuildSpec
  }

  public fun addSrcSpec(projectSrcSpec: ProjectSrcSpec) {
    this.projectSrcSpecs.add(projectSrcSpec)
  }

  override fun build(): ProjectSpec = ProjectSpec(
    gradlePath = gradlePath,
    projectSettingsSpec = projectSettingsSpec,
    projectBuildSpec = projectBuildSpec,
    subprojects = subprojects,
    projectSrcSpecs = projectSrcSpecs
  )
}

public fun Path.newFile(fileName: String): File = File(this.toFile(), fileName)
public fun File.newFile(fileName: String): File = File(this, fileName)
