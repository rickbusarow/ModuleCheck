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

plugins {
  id("mcbuild")
  id("com.gradle.plugin-publish") version "0.21.0"
  id("java-gradle-plugin")
}

mcbuild {
  artifactId = "com.rickbusarow.module-check"
  dagger = true
}

tasks.withType<Test> {
  // Gradle TestKit somewhat regularly runs out of memory on the freebie GitHub runners
  maxParallelForks = 1
}

dependencies {

  api(libs.javax.inject)
  api(libs.kotlin.compiler)
  api(libs.kotlinx.coroutines.core)
  api(libs.kotlinx.coroutines.jvm)
  api(libs.rickBusarow.dispatch.core)

  api(project(path = ":modulecheck-config:api"))
  api(project(path = ":modulecheck-core"))
  api(project(path = ":modulecheck-dagger"))
  api(project(path = ":modulecheck-parsing:gradle"))
  api(project(path = ":modulecheck-parsing:source"))
  api(project(path = ":modulecheck-parsing:wiring"))
  api(project(path = ":modulecheck-project:api"))
  api(project(path = ":modulecheck-reporting:logging"))
  api(project(path = ":modulecheck-rule:api"))
  api(project(path = ":modulecheck-runtime"))

  compileOnly(gradleApi())

  implementation(libs.agp)
  implementation(libs.kotlin.gradle.plug)
  implementation(libs.kotlin.gradle.plugin.api)
  implementation(libs.kotlin.reflect)
  implementation(libs.semVer)
  implementation(libs.square.anvil.gradle)

  implementation(project(path = ":modulecheck-project:impl"))
  implementation(project(path = ":modulecheck-rule:impl"))
  implementation(project(path = ":modulecheck-utils"))

  testImplementation(libs.bundles.hermit)
  testImplementation(libs.bundles.jUnit)
  testImplementation(libs.bundles.kotest)
  testImplementation(libs.kotlinPoet)

  testImplementation(project(path = ":modulecheck-internal-testing"))
  testImplementation(project(path = ":modulecheck-specs"))
  testImplementation(project(path = ":modulecheck-utils"))

  testImplementation(testFixtures(project(path = ":modulecheck-project:api")))
}

gradlePlugin {
  plugins {
    create("moduleCheck") {
      id = "com.rickbusarow.module-check"
      group = "com.rickbusarow.modulecheck"
      displayName = "ModuleCheck"
      implementationClass = "modulecheck.gradle.ModuleCheckPlugin"
      version = modulecheck.builds.VERSION_NAME
      description = "Fast dependency graph validation for gradle"
    }
  }
}

pluginBundle {
  website = "https://github.com/RBusarow/ModuleCheck"
  vcsUrl = "https://github.com/RBusarow/ModuleCheck"
  description = "Fast dependency graph validation for gradle"

  (plugins) {
    "moduleCheck" {
      displayName = "ModuleCheck"
      tags = listOf("kotlin", "dependencies", "android", "gradle-plugin", "kotlin-compiler-plugin")
    }
  }
}

val generatedDirPath = "$buildDir/generated/sources/buildProperties/kotlin/main"
sourceSets {
  main.configure {
    java.srcDir(project.file(generatedDirPath))
  }
}

val generateBuildProperties by tasks.registering {

  val version = modulecheck.builds.VERSION_NAME
  val sourceWebsite = modulecheck.builds.SOURCE_WEBSITE
  val docsWebsite = modulecheck.builds.DOCS_WEBSITE

  val buildPropertiesDir = File(generatedDirPath)
  val buildPropertiesFile = File(buildPropertiesDir, "modulecheck/gradle/task/BuildProperties.kt")

  inputs.file(rootProject.file("build-logic/mcbuild/src/main/kotlin/modulecheck/builds/publishing.kt"))
  outputs.file(buildPropertiesFile)

  doLast {

    buildPropertiesDir.mkdirs()

    buildPropertiesFile.writeText(
      """package modulecheck.gradle.task
      |
      |internal object BuildProperties {
      |  const val VERSION = "$version"
      |  const val SOURCE_WEBSITE = "$sourceWebsite"
      |  const val DOCS_WEBSITE = "$docsWebsite"
      |}
      |
      """.trimMargin()
    )
  }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>()
  .configureEach {

    dependsOn(generateBuildProperties)
  }
