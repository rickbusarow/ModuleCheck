package com.rickbusarow.modulecheck.testing

import java.nio.file.Path

class ProjectSettingsSpec private constructor(
  val includes: List<String>
) {
  fun writeIn(path: Path) {
    path.toFile().mkdirs()
    path.newFile("settings.gradle.kts").writeText(
      pluginManagement() +  includes()
    )
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
       |        useModule("com.android.tools.build:gradle:4.1.1")
       |      }
       |      if (requested.id.id.startsWith("org.jetbrains.kotlin")) {
       |        useVersion("1.4.21")
       |      }
       |    }
       |  }
       |}
       |
       |""".trimMargin()

  private fun includes() = includes.joinToString(",\n", "include(\n", "\n)") { "  \":$it\"" }

  class Builder {

    private val includes = mutableListOf<String>()

    fun addInclude(include: String) = apply {
      includes.add(include)
    }

    fun build() = ProjectSettingsSpec(includes)
  }
}
