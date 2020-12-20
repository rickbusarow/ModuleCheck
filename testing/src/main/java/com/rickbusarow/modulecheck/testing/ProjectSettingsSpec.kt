package com.rickbusarow.modulecheck.testing

import java.nio.file.Path

class ProjectSettingsSpec private constructor(
  val includes: List<String>
) {
  fun writeIn(path: Path) {
    path.toFile().mkdirs()
    path.newFile("settings.gradle.kts").writeText(
      includes.joinToString(",\n", "include(\n", "\n)") { "  \":$it\"" }
    )
  }

  class Builder {

    private val includes = mutableListOf<String>()

    fun addInclude(include: String) = apply {
      includes.add(include)
    }

    fun build() = ProjectSettingsSpec(includes)
  }
}
