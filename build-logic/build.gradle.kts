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

import org.jmailen.gradle.kotlinter.KotlinterExtension

buildscript {
  dependencies {
    classpath(libs.kotlin.gradle.plug)
    classpath(libs.vanniktech.publish)
  }
}

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlinter)
  alias(libs.plugins.dependencyAnalysis)
  alias(libs.plugins.moduleCheck)
}

moduleCheck {
  checks {
    sortDependencies = true
  }
}

val kotlinVersion = libs.versions.kotlin.get()
allprojects {

  configurations.all {
    resolutionStrategy {
      eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
          useVersion(kotlinVersion)
        }
      }
    }
  }

  afterEvaluate {
    configure<KotlinterExtension> {
      ignoreFailures = false
      reporters = arrayOf("checkstyle", "plain")
      experimentalRules = true
      disabledRules = arrayOf(
        "max-line-length", // manually formatting still does this, and KTLint will still wrap long chains when possible
        "filename", // same as Detekt's MatchingDeclarationName, but Detekt's version can be suppressed and this can't
        "argument-list-wrapping", // doesn't work half the time
        "no-empty-first-line-in-method-block", // code golf...
        // This can be re-enabled once 0.46.0 is released
        // https://github.com/pinterest/ktlint/issues/1435
        // "experimental:type-parameter-list-spacing",
        // added in 0.46.0
        "experimental:function-signature"
      )
    }

    configure<JavaPluginExtension> {
      @Suppress("MagicNumber")
      toolchain.languageVersion.set(JavaLanguageVersion.of(11))
    }
  }

  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {

      languageVersion = "1.6"
      apiVersion = "1.6"

      jvmTarget = "1.8"

      freeCompilerArgs = freeCompilerArgs + listOf(
        "-opt-in=kotlin.RequiresOptIn"
      )
    }
  }

  tasks.withType<org.jmailen.gradle.kotlinter.tasks.FormatTask> {

    // These exclude the Gradle-generated code from Kotlinter's checks.
    // These globs are relative to the source set's kotlin root.
    exclude("*Plugin.kt")
    exclude("gradle/kotlin/dsl/**")
  }
}

tasks.register("ktlintCheck") rootTask@{
  allprojects {
    this@rootTask.dependsOn(tasks.named("lintKotlin"))
  }
}
tasks.register("ktlintFormat") rootTask@{
  allprojects {
    this@rootTask.dependsOn(tasks.named("formatKotlin"))
  }
}

tasks.withType<Delete> rootTask@{

  subprojects {
    tasks.withType<Delete> subTask@{
      this@rootTask.dependsOn(this@subTask)
    }
  }
}

tasks.named("test") rootTask@{

  subprojects {
    tasks.withType<Test> subTask@{
      useJUnitPlatform()
      this@rootTask.dependsOn(this@subTask)
    }
  }
}
