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
  artifactId = "modulecheck-gradle-plugin"
  dagger = true
}

tasks.withType(Test::class.java)
  .configureEach {

    if (!System.getenv("CI").isNullOrBlank()) {
      // Gradle TestKit somewhat regularly runs out of memory on the freebie GitHub runners
      maxParallelForks = 1
    }

    dependsOn(rootProject.tasks.matching { it.name == "publishToMavenLocal" })
  }

dependencies {

  api(libs.javax.inject)

  api(project(path = ":modulecheck-config:api"))
  api(project(path = ":modulecheck-dagger"))
  api(project(path = ":modulecheck-finding:name"))
  api(project(path = ":modulecheck-gradle:platforms:api"))
  api(project(path = ":modulecheck-gradle:platforms:impl"))
  api(project(path = ":modulecheck-gradle:platforms:internal-android"))
  api(project(path = ":modulecheck-gradle:platforms:internal-jvm"))
  api(project(path = ":modulecheck-model:dependency:api"))
  api(project(path = ":modulecheck-parsing:gradle:dsl:api"))
  api(project(path = ":modulecheck-parsing:gradle:dsl:internal"))
  api(project(path = ":modulecheck-parsing:gradle:model:api"))
  api(project(path = ":modulecheck-parsing:gradle:model:impl-typesafe"))
  api(project(path = ":modulecheck-parsing:wiring"))
  api(project(path = ":modulecheck-project:api"))
  api(project(path = ":modulecheck-reporting:logging:api"))
  api(project(path = ":modulecheck-rule:api"))
  api(project(path = ":modulecheck-rule:impl-factory"))
  api(project(path = ":modulecheck-runtime:api"))

  compileOnly(gradleApi())

  compileOnly(libs.agp)
  compileOnly(libs.agp.api)
  compileOnly(libs.agp.builder.model)
  compileOnly(libs.kotlin.gradle.plug)
  compileOnly(libs.kotlin.gradle.plugin.api)
  compileOnly(libs.square.anvil.gradle)

  implementation(libs.google.dagger.api)
  implementation(libs.semVer)

  implementation(project(path = ":modulecheck-model:dependency:impl"))
  implementation(project(path = ":modulecheck-parsing:source:api"))
  implementation(project(path = ":modulecheck-project:impl"))
  implementation(project(path = ":modulecheck-rule:impl"))
  implementation(project(path = ":modulecheck-utils:stdlib"))

  testImplementation(libs.bundles.hermit)
  testImplementation(libs.bundles.jUnit)
  testImplementation(libs.bundles.kotest)
  testImplementation(libs.kotlinPoet)

  testImplementation(project(path = ":modulecheck-internal-testing"))
  testImplementation(project(path = ":modulecheck-project:testing"))
  testImplementation(project(path = ":modulecheck-utils:stdlib"))
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

val generateBuildProperties by tasks.registering generator@{

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
      """package modulecheck.gradle.internal
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

tasks.matching {
  it.name in setOf(
    "javaSourcesJar",
    "runKtlintCheckOverMainSourceSet",
    "runKtlintFormatOverMainSourceSet"
  )
}
  .configureEach {
    dependsOn(generateBuildProperties)
  }

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  dependsOn(generateBuildProperties)
}
