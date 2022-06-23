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

buildscript {
  dependencies {
    classpath(libs.kotlin.gradle.plug)
    classpath(libs.vanniktech.publish)
  }
}

// `alias(libs.______)` inside the plugins block throws a false positive warning
// https://youtrack.jetbrains.com/issue/KTIJ-19369
// There's also an IntelliJ plugin to disable this warning globally:
// https://plugins.jetbrains.com/plugin/18949-gradle-libs-error-suppressor
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  base
  alias(libs.plugins.ktlint)
}

allprojects {
  afterEvaluate {
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
      debug.set(false)

      outputToConsole.set(true)
      disabledRules.set(
        setOf(
          // manually formatting still does this,
          // and KTLint will still wrap long chains when possible
          "max-line-length",
          // same as Detekt's MatchingDeclarationName,
          // but Detekt's version can be suppressed and this can't
          "filename",
          // doesn't work half the time
          "experimental:argument-list-wrapping"
        )
      )
    }
  }
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
