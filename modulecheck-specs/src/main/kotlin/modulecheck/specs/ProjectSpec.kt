/*
 * Copyright (C) 2021-2022 Rick Busarow
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

package modulecheck.specs

import modulecheck.specs.ProjectSrcSpecBuilder.RawFile
import java.io.File
import java.nio.file.Path

public data class ProjectSpec(
  public var gradlePath: String,
  public var projectSettingsSpec: ProjectSettingsSpec?,
  public var projectBuildSpec: ProjectBuildSpec?,
  public val subprojects: MutableList<ProjectSpec>,
  public val projectSrcSpecs: MutableList<ProjectSrcSpec>,
  public val disableAutoManifest: Boolean
) {

  public fun toBuilder(): ProjectSpecBuilder = ProjectSpecBuilder(
    gradlePath = gradlePath,
    projectSettingsSpec = projectSettingsSpec,
    projectBuildSpec = projectBuildSpec,
    subprojects = subprojects,
    projectSrcSpecs = projectSrcSpecs,
    disableAutoManifest = disableAutoManifest
  )

  public inline fun edit(
    init: ProjectSpecBuilder.() -> Unit
  ): ProjectSpec = toBuilder().apply { init() }.build()

  public fun writeIn(path: Path) {
    maybeAddManifest()

    projectSettingsSpec?.writeIn(path)
    projectBuildSpec?.writeIn(path)
    subprojects.forEach { it.writeIn(Path.of(path.toString(), it.gradlePath)) }
    projectSrcSpecs.forEach { it.writeIn(path) }

    File(path.toFile(), "gradle").mkdirs()
    path.newFile("gradle/libs.versions.toml")
      .writeText(versionsToml)
  }

  // TODO - work on ProjectPoet and incorporate it here so I don't have to do these ugly hacks
  /**
   * If the project being written is an Android module, is missing a manifest,
   * and hasn't enabled [disableAutoManifest], then automatically write one so that it doesn't
   * need to be manually added in every test.
   *
   * The package name is derived by grabbing the maximum common package name out of all src files.
   */
  private fun maybeAddManifest() {

    if (disableAutoManifest) return
    if (projectBuildSpec?.android != true) return

    val hasManifest = projectSrcSpecs
      .asSequence()
      .flatMap { it.rawFiles }
      .any { it.fileName == "AndroidManifest.xml" }

    if (hasManifest) return

    val packageName = projectSrcSpecs
      .asSequence()
      .flatMap { it.fileSpecs }
      .map { it.packageName }
      .fold(null) { acc: String?, pName ->

        acc?.zip(pName)
          ?.takeWhile { it.first == it.second }
          ?.joinToString("") { it.first.toString() }
          ?.trimEnd('.')
          ?: pName
      }
      ?: "com.example.${gradlePath.split(':').last()}"

    edit {
      addSrcSpec(
        ProjectSrcSpec(Path.of("src/main")) {
          addRawFile(
            RawFile(
              "AndroidManifest.xml",
              "<manifest package=\"$packageName\" />"
            )
          )
        }
      )
    }
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

@Suppress("LongParameterList")
public class ProjectSpecBuilder(
  public var gradlePath: String,
  public var projectSettingsSpec: ProjectSettingsSpec? = null,
  public var projectBuildSpec: ProjectBuildSpec? = null,
  public val subprojects: MutableList<ProjectSpec> = mutableListOf(),
  public val projectSrcSpecs: MutableList<ProjectSrcSpec> = mutableListOf(),
  public var disableAutoManifest: Boolean = false,
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
    projectSrcSpecs = projectSrcSpecs,
    disableAutoManifest = disableAutoManifest
  )
}

public fun Path.newFile(fileName: String): File = File(this.toFile(), fileName)
public fun File.newFile(fileName: String): File = File(this, fileName)
