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

import java.nio.file.Path

@Suppress("MemberVisibilityCanBePrivate", "LongParameterList")
public class ProjectBuildSpec private constructor(
  public val plugins: List<String>,
  public val imports: List<String>,
  public val blocks: List<String>,
  public val repositories: List<String>,
  public val dependencies: List<String>,
  public val isAndroid: Boolean,
  public val buildScript: Boolean
) {

  public fun writeIn(path: Path) {
    path.toFile().mkdirs()
    path.newFile("build.gradle.kts")
      .writeText(
        imports() +
          buildScriptBlock() +
          pluginsBlock() +
          repositoriesBlock() +
          androidBlock() +
          dependenciesBlock() +
          blocksBlock()
      )
  }

  private fun pluginsBlock() = if (plugins.isEmpty()) "" else buildString {
    appendLine("plugins {")
    plugins.forEach { appendLine("  $it") }
    appendLine("}\n")
  }

  private fun imports() = if (imports.isEmpty()) "" else buildString {
    imports.forEach { appendLine(it) }
    appendLine()
  }

  private fun blocksBlock() = if (blocks.isEmpty()) "" else buildString {
    blocks.forEach { appendLine("$it\n") }
  }

  private fun repositoriesBlock() = if (repositories.isEmpty()) "" else buildString {
    appendLine("repositories {")
    repositories.forEach { appendLine("  $it") }
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
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.30")
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

  public class Builder internal constructor() {

    private val plugins = mutableListOf<String>()
    private val imports = mutableListOf<String>()
    private val blocks = mutableListOf<String>()
    private val repositories = mutableListOf<String>()
    private val dependencies = mutableListOf<String>()

    private var isAndroid = false
    private var isBuildScript = false

    public fun buildScript(): Builder = apply {
      isBuildScript = true
    }

    public fun android(): Builder = apply {
      isAndroid = true
    }

    public fun addImport(import: String): Builder = apply {
      imports.add(import)
    }

    public fun addBlock(codeBlock: String): Builder = apply {
      blocks.add(codeBlock)
    }

    public fun addPlugin(
      plugin: String,
      comment: String? = null,
      inlineComment: String? = null
    ): Builder = apply {
      val prev = comment?.let { "$it\n  " } ?: ""
      val after = inlineComment?.let { " $it" } ?: ""

      plugins.add("$prev$plugin$after")
    }

    public fun addRepository(
      repository: String,
      comment: String? = null,
      inlineComment: String? = null
    ): Builder = apply {
      val prev = comment?.let { "$it\n  " } ?: ""
      val after = inlineComment?.let { " $it" } ?: ""

      repositories.add("$prev$repository$after")
    }

    public fun addExternalDependency(
      configuration: String,
      dependencyPath: String,
      comment: String? = null,
      inlineComment: String? = null
    ): Builder = apply {
      val prev = comment?.let { "$it\n  " } ?: ""
      val after = inlineComment?.let { " $it" } ?: ""

      dependencies.add("$prev$configuration(\":$dependencyPath\")$after")
    }

    public fun addProjectDependency(
      configuration: String,
      dependencyPath: String,
      comment: String? = null,
      inlineComment: String? = null
    ): Builder = apply {
      val prev = comment?.let { "$it\n  " } ?: ""
      val after = inlineComment?.let { " $it" } ?: ""

      dependencies.add("$prev$configuration(project(path = \":$dependencyPath\"))$after")
    }

    public fun build(): ProjectBuildSpec =
      ProjectBuildSpec(
        plugins,
        imports,
        blocks,
        repositories,
        dependencies,
        isAndroid,
        isBuildScript
      )
  }

  public companion object {

    public fun builder(): Builder = Builder()
  }
}
