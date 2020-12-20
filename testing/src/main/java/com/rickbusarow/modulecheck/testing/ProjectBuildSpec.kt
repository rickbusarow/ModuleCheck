package com.rickbusarow.modulecheck.testing

import java.nio.file.Path

class ProjectBuildSpec private constructor(
  val plugins: List<String>,
  val dependencies: List<String>,
  val isAndroid: Boolean
) {

  fun writeIn(path: Path) {
    path.toFile().mkdirs()
    path.newFile("build.gradle.kts")
      .writeText(pluginsBlock() + androidBlock() + dependenciesBlock())
  }

  private fun pluginsBlock() = if (plugins.isEmpty()) "" else buildString {
    appendLine("plugins {")
    plugins.forEach { appendLine("  $it") }
    appendLine("}\n")
  }

  private fun androidBlock() = if (!isAndroid) "" else """android {
  compileSdkVersion(30)

  defaultConfig {
    minSdkVersion(23)
    targetSdkVersion(30)
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions {
    jvmTarget = "1.8"
  }
}

"""

  private fun dependenciesBlock() = if (dependencies.isEmpty()) "" else buildString {
    appendLine("dependencies {")
    dependencies.forEach { appendLine("  $it") }
    appendLine("}")
  }

  class Builder {

    private val plugins = mutableListOf<String>()
    private val dependencies = mutableListOf<String>()

    private var isAndroid = false

    fun android() = apply {
      isAndroid = true
    }

    fun addPlugin(plugin: String) = apply {
      plugins.add(plugin)
    }

    fun addDependency(configuration: String, dependencyPath: String) = apply {
      dependencies.add("$configuration(project(path = \":$dependencyPath\"))")
    }

    fun build() = ProjectBuildSpec(plugins, dependencies, isAndroid)
  }
}
