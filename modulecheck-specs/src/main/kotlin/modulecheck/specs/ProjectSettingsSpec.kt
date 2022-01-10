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

import java.nio.file.Path

public data class ProjectSettingsSpec(
  public var kotlinVersion: String = DEFAULT_KOTLIN_VERSION,
  public var agpVersion: String = DEFAULT_AGP_VERSION,
  public val includes: MutableList<String>
) {

  public fun toBuilder(): ProjectSettingsSpecBuilder = ProjectSettingsSpecBuilder(
    kotlinVersion = kotlinVersion,
    agpVersion = agpVersion,
    includes = includes
  )

  public inline fun edit(
    init: ProjectSettingsSpecBuilder.() -> Unit
  ): ProjectSettingsSpec = toBuilder().apply { init() }.build()

  public fun writeIn(path: Path) {
    path.toFile().mkdirs()
    path.newFile("settings.gradle.kts")
      .writeText(pluginManagement() + typeSafe() + includes())
  }

  private fun pluginManagement() =
    """pluginManagement {
       |  repositories {
       |    gradlePluginPortal()
       |    jcenter()
       |    google()
       |  }
       |  resolutionStrategy {
       |    eachPlugin {
       |      if (requested.id.id.startsWith("com.android")) {
       |        useModule("com.android.tools.build:gradle:$agpVersion")
       |      }
       |      if (requested.id.id.startsWith("org.jetbrains.kotlin")) {
       |        useVersion("$kotlinVersion")
       |      }
       |    }
       |  }
       |}
       |
       |""".trimMargin()

  private fun typeSafe() = """
    |enableFeaturePreview("VERSION_CATALOGS")
    |enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
    |
  """.trimMargin()

  private fun includes() = includes.joinToString(",\n", "include(\n", "\n)") { "  \":$it\"" }

  public companion object {

    public operator fun invoke(
      init: ProjectSettingsSpecBuilder.() -> Unit
    ): ProjectSettingsSpec = ProjectSettingsSpecBuilder(init = init).build()

    public fun builder(): ProjectSettingsSpecBuilder = ProjectSettingsSpecBuilder()
  }
}

public class ProjectSettingsSpecBuilder(
  public var kotlinVersion: String = DEFAULT_KOTLIN_VERSION,
  public var agpVersion: String = DEFAULT_AGP_VERSION,
  public val includes: MutableList<String> = mutableListOf(),
  init: ProjectSettingsSpecBuilder.() -> Unit = {}
) : Builder<ProjectSettingsSpec> {

  init {
    init()
  }

  public fun addInclude(include: String) {
    includes.add(include)
  }

  override fun build(): ProjectSettingsSpec = ProjectSettingsSpec(
    kotlinVersion = kotlinVersion,
    agpVersion = agpVersion,
    includes = includes
  )
}
