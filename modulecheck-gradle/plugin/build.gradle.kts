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

import modulecheck.builds.ShardTestTask
import modulecheck.builds.dependsOn
import modulecheck.builds.shards.registerYamlShardsTasks

plugins {
  id("mcbuild")
  id("com.gradle.plugin-publish")
  id("java-gradle-plugin")
  idea
}

val pluginId = "com.rickbusarow.module-check"

@Suppress("UnstableApiUsage")
val pluginDeclaration: NamedDomainObjectProvider<PluginDeclaration> = gradlePlugin.plugins
  .register("generateProtos") {
    id = pluginId
    group = modulecheck.builds.GROUP
    displayName = "ModuleCheck"
    implementationClass = "modulecheck.gradle.ModuleCheckPlugin"
    version = modulecheck.builds.VERSION_NAME
    description = "Fast dependency graph validation for gradle"
    tags.addAll("kotlin", "dependencies", "android", "gradle-plugin", "kotlin-compiler-plugin")
  }

mcbuild {
  published(
    artifactId = "modulecheck-gradle-plugin"
  )
  publishedPlugin(pluginDeclaration)
  dagger()

  buildConfig {
    packageName.set("modulecheck.gradle.internal")
    field("version") { modulecheck.builds.VERSION_NAME }
    field("sourceWebsite") { modulecheck.builds.SOURCE_WEBSITE }
    field("docsWebsite") { modulecheck.builds.DOCS_WEBSITE }
  }
  buildConfig("test") {
    packageName.set("modulecheck.gradle.internal")
    className.set("TestBuildProperties")
    field("version") { modulecheck.builds.VERSION_NAME }
    field("sourceWebsite") { modulecheck.builds.SOURCE_WEBSITE }
    field("docsWebsite") { modulecheck.builds.DOCS_WEBSITE }
    field("testKitDir") { "$buildDir/tmp/integrationTest/work/.gradle-test-kit" }
  }
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
          module.testSources.from(srcDir)
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
  api(libs.rickBusarow.dispatch.core)

  api(project(path = ":modulecheck-config:api"))
  api(project(path = ":modulecheck-dagger"))
  api(project(path = ":modulecheck-gradle:platforms:api"))
  api(project(path = ":modulecheck-model:dependency:api"))
  api(project(path = ":modulecheck-model:sourceset:api"))
  api(project(path = ":modulecheck-parsing:gradle:dsl:api"))
  api(project(path = ":modulecheck-parsing:wiring"))
  api(project(path = ":modulecheck-project:api"))
  api(project(path = ":modulecheck-reporting:logging:api"))
  api(project(path = ":modulecheck-rule:api"))
  api(project(path = ":modulecheck-runtime:api"))

  compileOnly(gradleApi())

  compileOnly(libs.agp)
  compileOnly(libs.agp.api)
  compileOnly(libs.agp.builder.model)
  compileOnly(libs.kotlin.gradle.plugin)
  compileOnly(libs.kotlin.gradle.plugin.api)
  compileOnly(libs.kotlinx.serialization.core)
  compileOnly(libs.square.anvil.gradle)

  implementation(libs.ajalt.mordant)
  implementation(libs.google.dagger.api)
  implementation(libs.semVer)

  implementation(project(path = ":modulecheck-config:impl"))
  implementation(project(path = ":modulecheck-finding:name"))
  implementation(project(path = ":modulecheck-gradle:platforms:impl"))
  implementation(project(path = ":modulecheck-gradle:platforms:internal-android"))
  implementation(project(path = ":modulecheck-gradle:platforms:internal-jvm"))
  implementation(project(path = ":modulecheck-model:dependency:impl"))
  implementation(project(path = ":modulecheck-parsing:gradle:dsl:internal"))
  implementation(project(path = ":modulecheck-parsing:kotlin-compiler:api"))
  implementation(project(path = ":modulecheck-parsing:kotlin-compiler:impl"))
  implementation(project(path = ":modulecheck-parsing:source:api"))
  implementation(project(path = ":modulecheck-project:impl"))
  implementation(project(path = ":modulecheck-rule:impl"))
  implementation(project(path = ":modulecheck-rule:impl-factory"))
  implementation(project(path = ":modulecheck-utils:coroutines:impl"))
  implementation(project(path = ":modulecheck-utils:stdlib"))

  "integrationTestImplementation"(project(path = ":modulecheck-config:impl"))
  "integrationTestImplementation"(project(path = ":modulecheck-gradle:platforms:api"))
  "integrationTestImplementation"(project(path = ":modulecheck-gradle:platforms:impl"))
  "integrationTestImplementation"(project(path = ":modulecheck-gradle:platforms:internal-android"))
  "integrationTestImplementation"(project(path = ":modulecheck-gradle:platforms:internal-jvm"))
  "integrationTestImplementation"(project(path = ":modulecheck-internal-testing"))
  "integrationTestImplementation"(project(path = ":modulecheck-model:dependency:impl"))
  "integrationTestImplementation"(project(path = ":modulecheck-model:sourceset:api"))
  "integrationTestImplementation"(project(path = ":modulecheck-parsing:gradle:dsl:internal"))
  "integrationTestImplementation"(project(path = ":modulecheck-parsing:kotlin-compiler:impl"))
  "integrationTestImplementation"(project(path = ":modulecheck-parsing:wiring"))
  "integrationTestImplementation"(project(path = ":modulecheck-project:api"))
  "integrationTestImplementation"(project(path = ":modulecheck-reporting:logging:api"))
  "integrationTestImplementation"(project(path = ":modulecheck-rule:api"))
  "integrationTestImplementation"(project(path = ":modulecheck-rule:impl"))
  "integrationTestImplementation"(project(path = ":modulecheck-rule:impl-factory"))
  "integrationTestImplementation"(project(path = ":modulecheck-utils:coroutines:impl"))
  "integrationTestImplementation"(project(path = ":modulecheck-utils:stdlib"))

  testImplementation(libs.bundles.junit)
  testImplementation(libs.bundles.kotest)

  testImplementation(project(path = ":modulecheck-internal-testing"))
  testImplementation(project(path = ":modulecheck-project-generation:api"))
}

val integrationTestTask = tasks.register("integrationTest", Test::class) {
  val integrationTestSourceSet = java.sourceSets["integrationTest"]
  testClassesDirs = integrationTestSourceSet.output.classesDirs
  classpath = integrationTestSourceSet.runtimeClasspath
  dependsOn(":publishToMavenLocalNoDokka")
}

val shardCount = 6
(1..shardCount).forEach {

  tasks.register("integrationTestShard$it", ShardTestTask::class) {
    shardNumber.set(it)
    totalShards.set(shardCount)
    testClassesDirs = integrationTest.get().output.classesDirs
    classpath = integrationTest.get().runtimeClasspath
    doFirst {
      setFilter()
    }
    dependsOn("integrationTestClasses", ":publishToMavenLocalNoDokka")
  }
}

registerYamlShardsTasks(
  shardCount = shardCount,
  startTagName = "### <start-integration-test-shards>",
  endTagName = "### <end-integration-test-shards>",
  taskNamePart = "integrationTest",
  yamlFile = rootProject.file(".github/workflows/ci.yml")
)

tasks.named("check").dependsOn(integrationTestTask)

kotlin {
  val compilations = target.compilations

  compilations.getByName("integrationTest") {
    associateWith(compilations.getByName("main"))
  }
}
