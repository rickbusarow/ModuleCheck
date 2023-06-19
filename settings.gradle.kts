/*
 * Copyright (C) 2021-2023 Rick Busarow
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

rootProject.name = "ModuleCheck"

pluginManagement {

  val allowMavenLocal = providers
    .gradleProperty("moduleCheck.allow-maven-local")
    .orNull.toBoolean()

  repositories {
    if (allowMavenLocal) {
      logger.lifecycle("${rootProject.name} -- allowing mavenLocal for plugins")
      mavenLocal()
    }
    gradlePluginPortal()
    mavenCentral()
    google()
    maven {
      url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
      content {
        includeGroup("com.rickbusarow.module-check")
        includeGroup("com.rickbusarow.modulecheck")
      }
    }
  }
  includeBuild("build-logic")
}

val allowMavenLocal = providers
  .gradleProperty("moduleCheck.allow-maven-local")
  .orNull.toBoolean()

dependencyResolutionManagement {
  @Suppress("UnstableApiUsage")
  repositories {
    if (allowMavenLocal) {
      logger.lifecycle("${rootProject.name} -- allowing mavenLocal for dependencies")
      mavenLocal()
    }
    mavenCentral()
    google()
    maven("https://plugins.gradle.org/m2/")
  }
}

plugins {
  id("com.gradle.enterprise") version "3.13.4"
}

val isCI = System.getenv("CI")?.toBoolean() == true

if (isCI) {
  printCiEnvironment()
}

gradleEnterprise {
  buildScan {

    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"

    publishAlways()

    tag(if (isCI) "CI" else "Local")

    val gitHubActions = System.getenv("GITHUB_ACTIONS")?.toBoolean() ?: false

    if (gitHubActions) {
      // ex: `octocat/Hello-World` as in github.com/octocat/Hello-World
      val repository = System.getenv("GITHUB_REPOSITORY")!!
      val runId = System.getenv("GITHUB_RUN_ID")!!

      link(
        "GitHub Action Run",
        "https://github.com/$repository/actions/runs/$runId"
      )
    }
  }
}

// Copy the root project's properties file to build-logic,
// to ensure that Gradle settings are identical and there's only 1 daemon.
// Note that with this copy, any changes to build-logic's properties file with just be overwritten.
// https://twitter.com/Louis_CAD/status/1498270951175299080?s=20&t=ZTgr-FiwhnjzGyNfDxfDlA
rootDir.resolve("gradle.properties")
  .copyTo(
    target = rootDir.resolve("build-logic/gradle.properties"),
    overwrite = true
  )

include(
  ":modulecheck-api",
  ":modulecheck-config:api",
  ":modulecheck-config:fake",
  ":modulecheck-config:impl",
  ":modulecheck-core",
  ":modulecheck-dagger",
  ":modulecheck-finding:api",
  ":modulecheck-finding:impl",
  ":modulecheck-finding:impl-android",
  ":modulecheck-finding:impl-sort",
  ":modulecheck-finding:name",
  ":modulecheck-gradle:platforms:api",
  ":modulecheck-gradle:platforms:impl",
  ":modulecheck-gradle:platforms:internal-android",
  ":modulecheck-gradle:platforms:internal-jvm",
  ":modulecheck-gradle:plugin",
  ":modulecheck-internal-testing",
  ":modulecheck-model:dependency:api",
  ":modulecheck-model:dependency:impl",
  ":modulecheck-model:sourceset:api",
  ":modulecheck-parsing:android",
  ":modulecheck-parsing:gradle:dsl:api",
  ":modulecheck-parsing:gradle:dsl:internal",
  ":modulecheck-parsing:gradle:dsl:precompiled",
  ":modulecheck-parsing:gradle:dsl:testing",
  ":modulecheck-parsing:gradle:model:api",
  ":modulecheck-parsing:gradle:model:impl-typesafe",
  ":modulecheck-parsing:groovy-antlr",
  ":modulecheck-parsing:java",
  ":modulecheck-parsing:kotlin-compiler:api",
  ":modulecheck-parsing:kotlin-compiler:impl",
  ":modulecheck-parsing:psi",
  ":modulecheck-parsing:source:api",
  ":modulecheck-parsing:source:testing",
  ":modulecheck-parsing:wiring",
  ":modulecheck-project:api",
  ":modulecheck-project:impl",
  ":modulecheck-project:testing",
  ":modulecheck-project-generation:api",
  ":modulecheck-reporting:checkstyle",
  ":modulecheck-reporting:console",
  ":modulecheck-reporting:graphviz",
  ":modulecheck-reporting:logging:api",
  ":modulecheck-reporting:logging:testing",
  ":modulecheck-reporting:sarif",
  ":modulecheck-rule:api",
  ":modulecheck-rule:impl",
  ":modulecheck-rule:impl-factory",
  ":modulecheck-rule:testing",
  ":modulecheck-runtime:api",
  ":modulecheck-runtime:testing",
  ":modulecheck-utils:cache",
  ":modulecheck-utils:coroutines:api",
  ":modulecheck-utils:coroutines:impl",
  ":modulecheck-utils:lazy",
  ":modulecheck-utils:stdlib",
  ":modulecheck-utils:trace",
  ":modulecheck-utils:trace-testing",
  ":modulecheck-utils:traversal"
)

fun printCiEnvironment() {
  fun Long.gigabytes(): String {
    return "%.1f GB".format(toDouble() / (1024 * 1024 * 1024)).padEnd(52)
  }

  val os = System.getProperty("os.name").toString().padEnd(52)
  val processors = Runtime.getRuntime().availableProcessors().toString().padEnd(52)
  val totalMemory = Runtime.getRuntime().totalMemory().gigabytes()
  val freeMemory = Runtime.getRuntime().freeMemory().gigabytes()
  val maxMemory = Runtime.getRuntime().maxMemory().gigabytes()
  println(
    """
    ╔══════════════════════════════════════════════════╗
    ║                  CI environment                  ║
    ║                                                  ║
    ║                     OS - $os                     ║
    ║   available processors - $processors             ║
    ║                                                  ║
    ║       allocated memory - $totalMemory            ║
    ║            free memory - $freeMemory             ║
    ║             max memory - $maxMemory              ║
    ╚══════════════════════════════════════════════════╝
    """.trimIndent()
      .lineSequence()
      .joinToString("\n") { line ->
        val sub = line.substringBeforeLast('║', "")
        if (sub.length > 51) sub.substring(0..50) + '║' else line
      }
  )
}
