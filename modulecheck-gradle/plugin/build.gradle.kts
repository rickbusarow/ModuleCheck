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
  idea
}

mcbuild {
  artifactId = "modulecheck-gradle-plugin"
  dagger()

  buildProperties(
    "main",
    """
    package modulecheck.gradle.internal

    internal class BuildProperties {
      val version = "${modulecheck.builds.VERSION_NAME}"
      val sourceWebsite = "${modulecheck.builds.SOURCE_WEBSITE}"
      val docsWebsite = "${modulecheck.builds.DOCS_WEBSITE}"
    }
    """
  )
}

val main by sourceSets.getting
val test by sourceSets.getting

val integrationTest by java.sourceSets.registering {
  kotlin.apply {
    compileClasspath += main.output
      .plus(test.output)
      .plus(configurations.testRuntimeClasspath.get())
    runtimeClasspath += output + compileClasspath
  }
}

// mark the integrationTest directory as a test directory in the IDE
idea {
  module {
    integrationTest.configure {
      allSource.srcDirs
        .forEach { srcDir ->
          module.testSourceDirs.add(srcDir)
        }
    }
  }
}

val integrationTestCompile by configurations.registering {
  extendsFrom(configurations["testCompileOnly"])
}
val integrationTestRuntime by configurations.registering {
  extendsFrom(configurations["testRuntimeOnly"])
}

dependencies {

  api(libs.javax.inject)

  api(project(path = ":modulecheck-config:api"))
  api(project(path = ":modulecheck-dagger"))
  api(project(path = ":modulecheck-finding:name"))
  api(project(path = ":modulecheck-gradle:platforms:api"))
  api(project(path = ":modulecheck-gradle:platforms:internal-jvm"))
  api(project(path = ":modulecheck-model:dependency:api"))
  api(project(path = ":modulecheck-parsing:gradle:dsl:api"))
  api(project(path = ":modulecheck-parsing:gradle:model:api"))
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

  implementation(project(path = ":modulecheck-config:impl"))
  implementation(project(path = ":modulecheck-gradle:platforms:impl"))
  implementation(project(path = ":modulecheck-gradle:platforms:internal-android"))
  implementation(project(path = ":modulecheck-model:dependency:impl"))
  implementation(project(path = ":modulecheck-parsing:gradle:dsl:internal"))
  implementation(project(path = ":modulecheck-parsing:gradle:model:impl-typesafe"))
  implementation(project(path = ":modulecheck-parsing:kotlin-compiler:impl"))
  implementation(project(path = ":modulecheck-parsing:source:api"))
  implementation(project(path = ":modulecheck-project:impl"))
  implementation(project(path = ":modulecheck-rule:impl"))
  implementation(project(path = ":modulecheck-utils:coroutines:impl"))
  implementation(project(path = ":modulecheck-utils:stdlib"))

  "integrationTestImplementation"(project(path = ":modulecheck-config:impl"))
  "integrationTestImplementation"(project(path = ":modulecheck-gradle:platforms:impl"))
  "integrationTestImplementation"(project(path = ":modulecheck-gradle:platforms:internal-android"))
  "integrationTestImplementation"(project(path = ":modulecheck-gradle:platforms:internal-jvm"))
  "integrationTestImplementation"(project(path = ":modulecheck-internal-testing"))
  "integrationTestImplementation"(project(path = ":modulecheck-model:dependency:impl"))
  "integrationTestImplementation"(project(path = ":modulecheck-model:sourceset:api"))
  "integrationTestImplementation"(project(path = ":modulecheck-parsing:gradle:dsl:internal"))
  "integrationTestImplementation"(project(path = ":modulecheck-parsing:gradle:model:impl-typesafe"))
  "integrationTestImplementation"(project(path = ":modulecheck-parsing:kotlin-compiler:impl"))
  "integrationTestImplementation"(project(path = ":modulecheck-parsing:wiring"))
  "integrationTestImplementation"(project(path = ":modulecheck-rule:api"))
  "integrationTestImplementation"(project(path = ":modulecheck-rule:impl"))
  "integrationTestImplementation"(project(path = ":modulecheck-rule:impl-factory"))
  "integrationTestImplementation"(project(path = ":modulecheck-utils:coroutines:impl"))
  "integrationTestImplementation"(project(path = ":modulecheck-utils:stdlib"))

  testImplementation(libs.bundles.hermit)
  testImplementation(libs.bundles.jUnit)
  testImplementation(libs.bundles.kotest)

  testImplementation(project(path = ":modulecheck-internal-testing"))
  testImplementation(project(path = ":modulecheck-project-generation:api"))
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

val integrationTestTask = tasks.register("integrationTest", Test::class) {
  val integrationTestSourceSet = java.sourceSets["integrationTest"]
  testClassesDirs = integrationTestSourceSet.output.classesDirs
  classpath = integrationTestSourceSet.runtimeClasspath
  dependsOn(rootProject.tasks.matching { it.name == "publishToMavenLocalNoDokka" })
}

tasks.matching { it.name == "check" }.all { dependsOn(integrationTestTask) }

kotlin {
  val compilations = target.compilations

  compilations.getByName("integrationTest") {
    associateWith(compilations.getByName("main"))
  }
}
