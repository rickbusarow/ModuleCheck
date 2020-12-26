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

import java.nio.file.Path

class ProjectBuildSpec private constructor(
  val plugins: List<String>,
  val dependencies: List<String>,
  val isAndroid: Boolean,
  val buildScript: Boolean
) {

  fun writeIn(path: Path) {
    path.toFile().mkdirs()
    path.newFile("build.gradle.kts")
      .writeText(buildScriptBlock() + pluginsBlock() + androidBlock() + dependenciesBlock())
  }

  private fun pluginsBlock() = if (plugins.isEmpty()) "" else buildString {
    appendLine("plugins {")
    plugins.forEach { appendLine("  $it") }
    appendLine("}\n")
  }

  private fun buildScriptBlock() = if (!buildScript) "" else """buildscript {
  repositories {
    mavenCentral()
    google()
    jcenter()
    maven("https://plugins.gradle.org/m2/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
  }
  dependencies {
    classpath("com.android.tools.build:gradle:4.1.1")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.21")
  }
}

allprojects {

  repositories {
    mavenCentral()
    google()
    jcenter()
    maven("https://plugins.gradle.org/m2/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
  }

}
"""

  private fun androidBlock() = if (!isAndroid) "" else """android {
  compileSdkVersion(30)

  defaultConfig {
    minSdkVersion(23)
    targetSdkVersion(30)
    versionCode = 1
    versionName = "1.0"
  }

  buildTypes {
    getByName("release") {
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
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
    private var isBuildScript = false

    fun buildScript() = apply {
      isBuildScript = true
    }

    fun android() = apply {
      isAndroid = true
    }

    fun addPlugin(plugin: String) = apply {
      plugins.add(plugin)
    }

    fun addExternalDependency(
      configuration: String,
      dependencyPath: String,
      comment: String = "",
      inlineComment: String = ""
    ) = apply { dependencies.add(
      "$configuration(\":$dependencyPath\")"
    )    }

    fun addProjectDependency(
      configuration: String,
      dependencyPath: String,
      comment: String = "",
      inlineComment: String = ""
    ) = apply {
      dependencies.add("$comment$configuration(project(path = \":$dependencyPath\"))")
    }

    fun build() = ProjectBuildSpec(plugins, dependencies, isAndroid, isBuildScript)
  }
}
