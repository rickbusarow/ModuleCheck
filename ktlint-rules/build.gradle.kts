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

import modulecheck.builds.DOCS_WEBSITE
import modulecheck.builds.SOURCE_WEBSITE
import modulecheck.builds.VERSION_NAME

// `alias(libs.______)` inside the plugins block throws a false positive warning
// https://youtrack.jetbrains.com/issue/KTIJ-19369
// There's also an IntelliJ plugin to disable this warning globally:
// https://plugins.jetbrains.com/plugin/18949-gradle-libs-error-suppressor
@Suppress("DSL_SCOPE_VIOLATION")

plugins {
  id("mcbuild")
}

mcbuild {
  ksp = true
}
val kotlinVersion = libs.versions.kotlin.get()

dependencies {
  api(libs.detekt.api)

  implementation(libs.google.auto.common)
  implementation(libs.google.auto.service.annotations)
  implementation(libs.google.ksp)
  implementation(libs.kotlin.compiler)
  implementation(libs.ktlint.core)
  implementation(libs.ktlint.gradle)
  implementation(libs.ktlint.ruleset.standard)

  "ksp"(libs.zacSweers.auto.service.ksp)

  testImplementation(libs.bundles.hermit)
  testImplementation(libs.bundles.jUnit)
  testImplementation(libs.bundles.kotest)
  testImplementation(libs.ktlint.test)
}

tasks.withType<Test> {
  useJUnitPlatform()
}

java {
  // This is different from the Kotlin jvm target.
  @Suppress("MagicNumber")
  toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions {

    languageVersion = "1.5"
    apiVersion = "1.5"

    jvmTarget = "11"

    freeCompilerArgs = freeCompilerArgs + listOf(
      "-opt-in=kotlin.RequiresOptIn"
    )
  }
}

val generatedDirPath = "$buildDir/generated/sources/buildProperties/kotlin/main"
sourceSets {
  main.configure {
    java.srcDir(project.file(generatedDirPath))
  }
}

val generateBuildProperties by tasks.registering {

  val buildPropertiesDir = File(generatedDirPath)
  val buildPropertiesFile = File(
    buildPropertiesDir, "modulecheck/builds/ktlint/BuildProperties.kt"
  )

  inputs.file(
    rootProject.file("build-logic/mcbuild/src/main/kotlin/modulecheck/builds/publishing.kt")
  )
  outputs.file(buildPropertiesFile)

  doLast {

    buildPropertiesDir.deleteRecursively()
    buildPropertiesFile.parentFile.mkdirs()

    buildPropertiesFile.writeText(
      """
      |/*
      | * Copyright (C) 2021-2022 Rick Busarow
      | * Licensed under the Apache License, Version 2.0 (the "License");
      | * you may not use this file except in compliance with the License.
      | * You may obtain a copy of the License at
      | *
      | *      http://www.apache.org/licenses/LICENSE-2.0
      | *
      | * Unless required by applicable law or agreed to in writing, software
      | * distributed under the License is distributed on an "AS IS" BASIS,
      | * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
      | * See the License for the specific language governing permissions and
      | * limitations under the License.
      | */
      |
      |package modulecheck.builds.ktlint
      |
      |internal class BuildProperties {
      |  val version = "$VERSION_NAME"
      |  val sourceWebsite = "$SOURCE_WEBSITE"
      |  val docsWebsite = "$DOCS_WEBSITE"
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

val jarTask = tasks.withType<Jar>()

rootProject.tasks.named("prepareKotlinBuildScriptModel") {
  dependsOn(generateBuildProperties, jarTask)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>()
  .configureEach {
    dependsOn(generateBuildProperties)
  }
