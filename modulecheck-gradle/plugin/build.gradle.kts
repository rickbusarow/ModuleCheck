/*
 * Copyright (C) 2021-2025 Rick Busarow
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

import org.gradle.api.tasks.testing.logging.TestLogEvent
import kotlin.math.ceil

plugins {
  alias(libs.plugins.mahout.java.gradle.plugin)
  alias(libs.plugins.gradle.plugin.publish)
  alias(libs.plugins.buildconfig)
  kotlin("kapt")
  idea
}

val pluginId = "com.rickbusarow.module-check"

val pluginDeclaration: NamedDomainObjectProvider<PluginDeclaration> = gradlePlugin.plugins
  .register("moduleCheck") {
    id = pluginId
    group = mahoutProperties.group.get()
    displayName = "ModuleCheck"
    implementationClass = "modulecheck.gradle.ModuleCheckPlugin"
    description = "Fast dependency graph validation for gradle"
    tags.addAll("kotlin", "dependencies", "android", "gradle-plugin", "kotlin-compiler-plugin")
  }

mahout {
  publishing {
    pluginMaven(
      artifactId = "modulecheck-gradle-plugin"
    )
    publishPlugin(pluginDeclaration)
  }
  gradleTests()
}

buildConfig {
  val DOCS_WEBSITE = "https://rickbusarow.github.io/ModuleCheck"
  sourceSets.named("main") {
    packageName.set("modulecheck.gradle.internal")
    buildConfigField("version", mahoutProperties.versionName)
    buildConfigField("sourceWebsite", mahoutProperties.publishing.pom.url)
    buildConfigField("docsWebsite", DOCS_WEBSITE)
  }
}

val gradleTestImplementation by configurations.getting

dependencies {

  api(libs.javax.inject)
  api(libs.rickBusarow.dispatch.core)

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
  api(project(path = ":modulecheck-runtime:api"))

  compileOnly(gradleApi())

  compileOnly(libs.agp)
  compileOnly(libs.agp.api)
  compileOnly(libs.agp.builder.model)
  compileOnly(libs.kotlin.gradle.plugin)
  compileOnly(libs.kotlin.gradle.plugin.api)
  compileOnly(libs.square.anvil.gradle)

  implementation(libs.ajalt.mordant)
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
  implementation(project(path = ":modulecheck-rule:impl-factory"))
  implementation(project(path = ":modulecheck-utils:coroutines:impl"))
  implementation(project(path = ":modulecheck-utils:stdlib"))

  gradleTestImplementation(libs.bundles.junit)
  gradleTestImplementation(libs.bundles.kotest)

  gradleTestImplementation(project(path = ":modulecheck-config:api"))
  gradleTestImplementation(project(path = ":modulecheck-config:impl"))
  gradleTestImplementation(project(path = ":modulecheck-dagger"))
  gradleTestImplementation(project(path = ":modulecheck-finding:name"))
  gradleTestImplementation(project(path = ":modulecheck-gradle:platforms:api"))
  gradleTestImplementation(project(path = ":modulecheck-gradle:platforms:impl"))
  gradleTestImplementation(project(path = ":modulecheck-gradle:platforms:internal-android"))
  gradleTestImplementation(project(path = ":modulecheck-gradle:platforms:internal-jvm"))
  gradleTestImplementation(project(path = ":modulecheck-internal-testing"))
  gradleTestImplementation(project(path = ":modulecheck-model:dependency:api"))
  gradleTestImplementation(project(path = ":modulecheck-model:dependency:impl"))
  gradleTestImplementation(project(path = ":modulecheck-model:sourceset:api"))
  gradleTestImplementation(project(path = ":modulecheck-parsing:gradle:dsl:api"))
  gradleTestImplementation(project(path = ":modulecheck-parsing:gradle:dsl:internal"))
  gradleTestImplementation(project(path = ":modulecheck-parsing:gradle:model:api"))
  gradleTestImplementation(project(path = ":modulecheck-parsing:gradle:model:impl-typesafe"))
  gradleTestImplementation(project(path = ":modulecheck-parsing:kotlin-compiler:impl"))
  gradleTestImplementation(project(path = ":modulecheck-parsing:source:api"))
  gradleTestImplementation(project(path = ":modulecheck-parsing:wiring"))
  gradleTestImplementation(project(path = ":modulecheck-project-generation:api"))
  gradleTestImplementation(project(path = ":modulecheck-project:api"))
  gradleTestImplementation(project(path = ":modulecheck-project:impl"))
  gradleTestImplementation(project(path = ":modulecheck-reporting:logging:api"))
  gradleTestImplementation(project(path = ":modulecheck-rule:api"))
  gradleTestImplementation(project(path = ":modulecheck-rule:impl"))
  gradleTestImplementation(project(path = ":modulecheck-rule:impl-factory"))
  gradleTestImplementation(project(path = ":modulecheck-runtime:api"))
  gradleTestImplementation(project(path = ":modulecheck-utils:coroutines:impl"))
  gradleTestImplementation(project(path = ":modulecheck-utils:stdlib"))

  kapt(libs.google.dagger.compiler)
}

val gradleTestTask by tasks.named("gradleTest", Test::class)

// val shardCount = 6
// (1..shardCount).forEach {
//
//   tasks.register("gradleTestShard$it", ShardTestTask::class) {
//     shardNumber.set(it)
//     totalShards.set(shardCount)
//     testClassesDirs = gradleTest.get().output.classesDirs
//     classpath = gradleTest.get().runtimeClasspath
//     doFirst {
//       setFilter()
//     }
//     dependsOn("gradleTestClasses", ":publishToMavenLocalNoDokka")
//   }
// }

// registerYamlShardsTasks(
//   shardCount = shardCount,
//   startTagName = "### <start-integration-test-shards>",
//   endTagName = "### <end-integration-test-shards>",
//   taskNamePart = "gradleTest",
//   yamlFile = rootProject.file(".github/workflows/ci.yml")
// )

abstract class ShardTestTask : Test() {

  @get:Input
  abstract val totalShards: Property<Int>

  @get:Input
  abstract val shardNumber: Property<Int>

  private var filterWasSet: Boolean = false

  fun setFilter() {

    // Calculate the range of test classes for this shard
    val testClassCount = testClassesDirs.asFileTree.matching {
      include("**/*Test.class")
    }.files.size

    val testsPerShard = ceil(testClassCount.toDouble() / totalShards.get()).toInt()
    val startIndex = testsPerShard * (shardNumber.get() - 1)
    val endIndex = minOf(testClassCount, startIndex + testsPerShard)

    testLogging.events(
      TestLogEvent.FAILED,
      TestLogEvent.STARTED,
      TestLogEvent.PASSED,
      TestLogEvent.SKIPPED
    )

    testClassesDirs.asFileTree.matching {
      include("**/*Test.class")
    }.files.asSequence()
      .sorted()
      .map { file -> file.name.replace(".class", "") }
      .drop(startIndex)
      .take(endIndex - startIndex)
      .also {

        println(
          "###### integration test shard ${shardNumber.get()} of ${totalShards.get()} includes:\n" +
            it.joinToString("\n")
        )
      }
      .forEach {
        this@ShardTestTask.filter.includeTest(it, null)
      }

    filterWasSet = true
  }

  @TaskAction
  fun execute() {

    if (!filterWasSet) {
      throw GradleException("This shard test task did not have its filter set.")
    }
  }
}
