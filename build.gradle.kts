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
    classpath(libs.ktlint.gradle)
    classpath(libs.vanniktech.publish)
  }
}

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  alias(libs.plugins.dependencyAnalysis)
  alias(libs.plugins.detekt)
  alias(libs.plugins.gradleDoctor)
  alias(libs.plugins.moduleCheck)
  alias(libs.plugins.taskTree)
  base
  id("mcbuild.root")
}

moduleCheck {
  deleteUnused = true
  checks {
    depths = true
    sortDependencies = true
  }
  reports {
    depths.enabled = true
    graphs {
      enabled = true
      outputDir = "$buildDir/reports/modulecheck/graphs"
    }
  }
}

afterEvaluate {

  // Hack for ensuring that when 'publishToMavenLocal' is invoked from the root project,
  // all subprojects are published.  This is used in plugin tests.
  sequenceOf(
    "publishToMavenLocal",
    "publishToMavenLocalNoDokka"
  ).forEach { taskName ->
    tasks.register(taskName) {
      subprojects.forEach { sub ->
        dependsOn(sub.tasks.matching { it.name == taskName })
      }
    }
  }

  sequenceOf(
    "buildHealth",
    "clean",
    "ktlintCheck",
    "ktlintFormat",
    "moduleCheckSortDependenciesAuto",
    "test"
  ).forEach { taskName ->
    tasks.named(taskName).configure {
      dependsOn(gradle.includedBuild("build-logic").task(":$taskName"))
    }
  }
}

doctor {
  disallowCleanTaskDependencies.set(false)
  javaHome {
    // this is throwing a false positive
    // JAVA_HOME is /Users/rbusarow/Library/Java/JavaVirtualMachines/azul-11-ARM64
    // Gradle is using /Users/rbusarow/Library/Java/JavaVirtualMachines/azul-11-ARM64/zulu-11.jdk/Contents/Home
    ensureJavaHomeMatches.set(false)
  }
}
