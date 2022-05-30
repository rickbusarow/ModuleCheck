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

package modulecheck.gradle

import io.kotest.matchers.string.shouldContain
import modulecheck.gradle.internal.BuildProperties
import modulecheck.utils.remove
import org.junit.jupiter.api.TestFactory

class RuntimeClasspathValidationTest : BaseGradleTest() {

  @TestFactory
  fun `all tasks should succeed without any other libraries in the build classpath`() =
    testProjectVersions()
      .flatMap { versions ->
        listOf(
          "moduleCheckAuto",
          "moduleCheckDepths",
          "moduleCheckGraphs",
          "moduleCheckSortDependenciesAuto",
          "moduleCheckSortPluginsAuto"
        ).map { taskName ->
          taskName to versions
        }
      }
      .dynamic(
        filter = { second.isValid() },
        testName = { it.second.toString() + " - ${it.first}" },
        setup = { (_, versions) ->
          agpVersion = versions.agpVersion
          gradleVersion = versions.gradleVersion
          kotlinVersion = versions.kotlinVersion
        }
      ) { (taskName, _) ->
        rootBuild.writeText(
          """
          plugins {
            id("com.rickbusarow.module-check")
          }
          """
        )
        // This is the same settings as the default used in other tests,
        // except no `google()` repository.  `google()` shouldn't be necessary since there should be
        // no dependency upon AGP.
        rootSettings.writeText(
          """
          rootProject.name = "root"

          pluginManagement {
            repositories {
              gradlePluginPortal()
              mavenCentral()
              mavenLocal()
            }
            resolutionStrategy {
              eachPlugin {
                if (requested.id.id.startsWith("com.android")) {
                  useVersion("$agpVersion")
                }
                if (requested.id.id == "com.rickbusarow.module-check") {
                  useVersion("${BuildProperties.VERSION}")
                }
                if (requested.id.id.startsWith("org.jetbrains.kotlin")) {
                  useVersion("$kotlinVersion")
                }
                if (requested.id.id == "com.squareup.anvil") {
                  useVersion("$anvilVersion")
                }
              }
            }
          }
          dependencyResolutionManagement {
            @Suppress("UnstableApiUsage")
            repositories {
              mavenCentral()
              mavenLocal()
            }
          }
          """.trimIndent()
        )

        // just double-check that this settings file is in sync with the default
        rootSettings shouldHaveText DEFAULT_SETTINGS_FILE.remove("\\s*google\\(\\)".toRegex())

        shouldSucceed(taskName)
      }

  @TestFactory
  fun `test project should have its own versions for AGP and KGP in classpath`() =
    testProjectVersions()
      // These tests aren't affected by Gradle
      .filter { it.gradleVersion == gradleVersion }
      .dynamic(
        filter = { isValid() },
        testName = { it.toString() },
        setup = { versions ->
          agpVersion = versions.agpVersion
          gradleVersion = versions.gradleVersion
          kotlinVersion = versions.kotlinVersion
        }
      ) {
        rootBuild.writeText(
          """
        plugins {
          id("com.android.library")
          kotlin("android")
          id("com.rickbusarow.module-check")
        }

        android {
          defaultConfig {
            minSdkVersion(23)
            compileSdkVersion(30)
            targetSdkVersion(30)
          }
        }

        val printClasspath by tasks.registering {
          doLast {
            project.buildscript
              .configurations["classpath"]!!
              .allDependencies.forEach {
                println("${'$'}{it.group}:${'$'}{it.name}:${'$'}{it.version}")
              }
          }
        }
        """
        )

        shouldSucceed("printClasspath") {

          output shouldContain "com.android.library:com.android.library.gradle.plugin:$agpVersion"
          output shouldContain "org.jetbrains.kotlin.android:org.jetbrains.kotlin.android.gradle.plugin:$kotlinVersion"
        }
      }

  @TestFactory
  fun `test project should build with any kotlin version`() = kotlin {
    kotlinProject(":lib1") {
      buildFile {
        """
        plugins {
          kotlin("jvm")
        }
        """
      }

      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
      )
    }

    shouldSucceed("compileKotlin")
  }
}
