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

buildscript {
  dependencies {
    classpath(libs.kotlin.gradle.plug)
    classpath(libs.vanniktech.publish)
  }
}

@Suppress("UnstableApiUsage")
plugins {
  base
  alias(libs.plugins.ktlint)
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
  debug.set(false)

  outputToConsole.set(true)
  disabledRules.set(
    setOf(
      "max-line-length", // manually formatting still does this, and KTLint will still wrap long chains when possible
      "filename", // same as Detekt's MatchingDeclarationName, but Detekt's version can be suppressed and this can't
      "experimental:argument-list-wrapping" // doesn't work half the time
    )
  )
}
tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask> {
  workerMaxHeapSize.set("512m")
}

tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask> rootTask@{

  val isFormat = this@rootTask.name.contains("Format", ignoreCase = true)

  subprojects {
    tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask> subTask@{
      val subIsFormat = this@subTask.name.contains("Format", ignoreCase = true) == isFormat

      if (subIsFormat == isFormat) {
        this@rootTask.dependsOn(this@subTask)
      }
    }
  }
}

tasks.withType<Delete> rootTask@{

  subprojects {
    tasks.withType<Delete> subTask@{
      this@rootTask.dependsOn(this@subTask)
    }
  }
}
