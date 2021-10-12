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

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.detekt

buildscript {
  repositories {
    mavenCentral()
    google()
    maven("https://plugins.gradle.org/m2/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
  }
  dependencies {
    classpath("com.android.tools.build:gradle:7.0.3")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.31")
    classpath("org.jetbrains.kotlinx:kotlinx-knit:0.3.0")
    classpath("com.vanniktech:gradle-maven-publish-plugin:0.18.0")
    classpath("org.jlleitschuh.gradle:ktlint-gradle:10.2.0")
  }
}

plugins {
  id("com.autonomousapps.dependency-analysis") version "0.78.0"
  id("com.github.ben-manes.versions") version "0.39.0"
  id("io.gitlab.arturbosch.detekt") version "1.18.1"
  id("org.jetbrains.dokka") version "1.5.31"
  id("com.osacky.doctor") version "0.7.3"
  id("com.dorongold.task-tree") version "2.1.0"
  base
}

allprojects {

  repositories {
    google()
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://jitpack.io")
    maven("https://s3-us-west-2.amazonaws.com/si-mobile-sdks/android/")
  }
}

@Suppress("DEPRECATION")
detekt {

  parallel = true
  config = files("$rootDir/detekt/detekt-config.yml")

  reports {
    xml.enabled = false
    html.enabled = true
    txt.enabled = false
  }
}

tasks.withType<DetektCreateBaselineTask> {

  setSource(files(rootDir))

  include("**/*.kt", "**/*.kts")
  exclude("**/resources/**", "**/build/**", "**/src/test/java**")

  // Target version of the generated JVM bytecode. It is used for type resolution.
  this.jvmTarget = "1.8"
}

tasks.withType<Detekt> {

  setSource(files(rootDir))

  include("**/*.kt", "**/*.kts")
  exclude("**/resources/**", "**/build/**", "**/src/test/java**", "**/src/test/kotlin**")

  // Target version of the generated JVM bytecode. It is used for type resolution.
  this.jvmTarget = "1.8"
}

fun isNonStable(version: String): Boolean {
  val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
  val regex = "^[0-9,.v-]+(-r)?$".toRegex()
  val isStable = stableKeyword || regex.matches(version)
  return isStable.not()
}

tasks.named(
  "dependencyUpdates",
  com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask::class.java
).configure {
  rejectVersionIf {
    isNonStable(candidate.version) && !isNonStable(currentVersion)
  }
}

allprojects {
  apply(plugin = "org.jlleitschuh.gradle.ktlint")

  configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    debug.set(false)

    disabledRules.set(
      kotlin.collections.setOf(
        "no-multi-spaces",
        "no-wildcard-imports",
        "max-line-length", // manually formatting still does this, and KTLint will still wrap long chains when possible
        "filename", // same as Detekt's MatchingDeclarationName, but Detekt's version can be suppressed and this can't
        "experimental:argument-list-wrapping" // doesn't work half the time
      )
    )
  }
  tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask> {
    workerMaxHeapSize.set("512m")
  }
}

val kotlinVersion = libs.versions.kotlin.get()

allprojects {
  configurations.all {
    resolutionStrategy {
      force("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
      eachDependency {
        when {
          requested.group == "org.jetbrains.kotlin" -> useVersion(kotlinVersion)
        }
      }
    }
  }
}

// Delete any empty directories while cleaning.
allprojects {
  val proj = this@allprojects

  proj.tasks
    .withType<Delete>()
    .configureEach {
      doLast {

        val subprojectDirs = proj.subprojects
          .map { it.projectDir.path }

        proj.projectDir.walkBottomUp()
          .filter { it.isDirectory }
          .filterNot { dir -> subprojectDirs.any { dir.path.startsWith(it) } }
          .filterNot { it.path.contains(".gradle") }
          .filter { it.listFiles()?.isEmpty() != false }
          .forEach { it.deleteRecursively() }
      }
    }

  val lintMain by tasks.registering {

    doFirst {
      tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>()
        .configureEach {
          kotlinOptions {
            allWarningsAsErrors = true
          }
        }
    }
  }
  lintMain {
    finalizedBy("compileKotlin")
  }

  tasks.withType<Test> {

    project
      .properties
      .asSequence()
      .filter { (key, value) ->
        key.startsWith("modulecheck") && value != null
      }
      .forEach { (key, value) ->
        systemProperty(key, value!!)
      }
  }

  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>()
    .configureEach {

      kotlinOptions {

        // disabled due to different Kotlin versions in the classpath
        allWarningsAsErrors = false

        jvmTarget = "1.8"

        freeCompilerArgs = freeCompilerArgs + listOf(
          "-Xinline-classes",
          "-Xopt-in=kotlin.ExperimentalStdlibApi",
          "-Xopt-in=kotlin.contracts.ExperimentalContracts"
        )
      }
    }
}

/**
 * Looks for all references to Tangle artifacts in the md/mdx files
 * in the un-versioned /website/docs. Updates all versions to the pre-release version.
 */
val updateWebsiteNextDocsVersionRefs by tasks.registering {

  description = "Updates the \"next\" version docs to use the next artifact version"
  group = "website"

  doLast {

    val version = project.extra.properties["VERSION_NAME"] as String

    fileTree("$rootDir/website/docs") {
      include("**/*.md*")
    }
      .forEach { file ->
        file.updateTangleVersionRef(version)
      }
  }
}

/**
 * Updates the "tangle" version in package.json
 */
val updateWebsitePackageJsonVersion by tasks.registering {

  description = "Updates the \"Tangle\" version in package.json"
  group = "website"

  doLast {

    val version = project.extra.properties["VERSION_NAME"] as String

    // this isn't very robust, but it's fine for this use-case
    val versionReg = """(\s*"version"\s*:\s*")[^"]*("\s*,)""".toRegex()

    // just in case some child object gets a "version" field, ignore it.
    // This only works if the correct field comes first (which it does).
    var foundOnce = false

    with(File("$rootDir/website/package.json")) {
      val newText = readText()
        .lines()
        .joinToString("\n") { line ->

          line.replace(versionReg) { matchResult ->

            if (!foundOnce) {
              foundOnce = true
              val (prefix, suffix) = matchResult.destructured
              "$prefix$version$suffix"
            } else {
              line
            }
          }
        }
      writeText(newText)
    }
  }
}

/**
 * Looks for all references to Tangle artifacts in the project README.md
 * to the current released version.
 */
val updateProjectReadmeVersionRefs by tasks.registering {

  description =
    "Updates the project-level README to use the latest published version in maven coordinates"
  group = "documentation"

  doLast {

    val version = project.extra.properties["VERSION_NAME"] as String

    File("$rootDir/README.md")
      .updateTangleVersionRef(version)
  }
}

fun File.updateTangleVersionRef(version: String) {

  val group = project.extra.properties["GROUP"] as String

  val pluginId = rootProject.extra.properties["PLUGIN_ID"] as String

  val pluginRegex =
    """^([^'"\n]*['"])$pluginId[^'"]*(['"].*) version (['"])[^'"]*(['"].*)${'$'}""".toRegex()
  val moduleRegex = """^([^'"\n]*['"])$group:([^:]*):[^'"]*(['"].*)${'$'}""".toRegex()

  val newText = readText()
    .lines()
    .joinToString("\n") { line ->
      line
        .replace(pluginRegex) { matchResult ->

          val (preId, postId, preVersion, postVersion) = matchResult.destructured

          "$preId$pluginId$postId version $preVersion$version$postVersion"
        }
        .replace(moduleRegex) { matchResult ->

          val (config, module, suffix) = matchResult.destructured

          "$config$group:$module:$version$suffix"
        }
    }

  writeText(newText)
}

val startSite by tasks.registering(Exec::class) {

  description = "launches the local development website"
  group = "website"

  dependsOn(
    versionDocs,
    updateWebsiteChangelog,
    updateWebsiteNextDocsVersionRefs,
    updateWebsitePackageJsonVersion
  )

  workingDir("$rootDir/website")
  commandLine("npm", "run", "start")
}

val versionDocs by tasks.registering(Exec::class) {

  description =
    "creates a new version snapshot of website docs, using the current version defined in gradle.properties"
  group = "website"

  val existingVersions = with(File("$rootDir/website/versions.json")) {
    "\"([^\"]*)\"".toRegex()
      .findAll(readText())
      .flatMap { it.destructured.toList() }
  }

  val version = project.extra.properties["VERSION_NAME"] as String

  enabled = version !in existingVersions

  workingDir("$rootDir/website")
  commandLine("npm", "run", "docusaurus", "docs:version", version)
}

val updateWebsiteChangelog by tasks.registering(Copy::class) {

  description = "copies the root project's CHANGELOG to the website and updates its formatting"
  group = "website"

  from("CHANGELOG.md")
  into("$rootDir/website/src/pages")

  doLast {

    // add one hashmark to each header, because GitHub and Docusaurus render them differently
    val changelog = File("$rootDir/website/src/pages/CHANGELOG.md")

    val newText = changelog.readText()
      .lines()
      .joinToString("\n") { line ->
        line.replace("^(#+) (.*)".toRegex()) { matchResult ->
          val (hashes, text) = matchResult.destructured

          "$hashes# $text"
        }
      }
    changelog.writeText(newText)
  }
}
