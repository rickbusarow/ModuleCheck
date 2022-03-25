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

import modulecheck.specs.DEFAULT_AGP_VERSION
import modulecheck.specs.DEFAULT_KOTLIN_VERSION
import modulecheck.testing.createSafely
import modulecheck.utils.child
import org.junit.jupiter.api.Test

class AndroidBuildConfigAvoidanceTest : BasePluginTest() {

  @Test
  fun `buildConfig task should be required when not configured in android library module`() {
    androidLibrary(":", "com.modulecheck.lib1") {
      buildFile {
        """
        plugins {
          id("com.android.library") version "$DEFAULT_AGP_VERSION"
          kotlin("android") version "$DEFAULT_KOTLIN_VERSION"
          id("com.rickbusarow.module-check")
        }
        repositories {
          mavenCentral()
          google()
          jcenter()
          maven("https://plugins.gradle.org/m2/")
          maven("https://oss.sonatype.org/content/repositories/snapshots")
        }

        android {
          defaultConfig {
            minSdkVersion(23)
            compileSdkVersion(30)
            targetSdkVersion(30)
          }
        }
        """
      }

      projectDir.child("settings.gradle.kts").createSafely()
    }

    shouldSucceed("moduleCheck").apply {
      // Assert that nothing else executed.
      // If ModuleCheck were relying upon buildConfig tasks, they'd be in this list.
      tasks.map { it.path } shouldBe listOf(
        ":preBuild",
        ":preDebugAndroidTestBuild",
        ":preDebugBuild",
        ":extractDeepLinksDebug",
        ":processDebugManifest",
        ":generateDebugBuildConfig",
        ":preReleaseBuild",
        ":generateReleaseBuildConfig",
        ":processDebugAndroidTestManifest",
        ":generateDebugAndroidTestBuildConfig",
        ":moduleCheck"
      )
    }
  }

  @Test
  fun `buildConfig task should not be required when disabled in android library module`() {
    androidLibrary(":", "com.modulecheck.lib1") {
      buildFile {
        """
        plugins {
          id("com.android.library") version "$DEFAULT_AGP_VERSION"
          kotlin("android") version "$DEFAULT_KOTLIN_VERSION"
          id("com.rickbusarow.module-check")
        }
        repositories {
          mavenCentral()
          google()
          jcenter()
          maven("https://plugins.gradle.org/m2/")
          maven("https://oss.sonatype.org/content/repositories/snapshots")
        }

        android {
          defaultConfig {
            minSdkVersion(23)
            compileSdkVersion(30)
            targetSdkVersion(30)
          }
          buildFeatures.buildConfig = false
        }
        """
      }

      projectDir.child("settings.gradle.kts").createSafely()
    }

    shouldSucceed("moduleCheck").apply {
      // Assert that nothing else executed.
      // If ModuleCheck were relying upon buildConfig tasks, they'd be in this list.
      tasks.map { it.path } shouldBe listOf(":moduleCheck")
    }
  }

  @Test
  fun `buildConfig task should not be required when disabled in android dynamic-feature module`() {
    androidLibrary(":", "com.modulecheck.lib1") {
      buildFile {
        """
        plugins {
          id("com.android.dynamic-feature") version "$DEFAULT_AGP_VERSION"
          kotlin("android") version "$DEFAULT_KOTLIN_VERSION"
          id("com.rickbusarow.module-check")
        }
        repositories {
          mavenCentral()
          google()
          jcenter()
          maven("https://plugins.gradle.org/m2/")
          maven("https://oss.sonatype.org/content/repositories/snapshots")
        }

        android {
          defaultConfig {
            minSdkVersion(23)
            compileSdkVersion(30)
          }
          buildFeatures.buildConfig = false
        }
        """
      }

      projectDir.child("settings.gradle.kts").createSafely()
    }

    shouldSucceed("moduleCheck").apply {
      // Assert that nothing else executed.
      // If ModuleCheck were relying upon buildConfig tasks, they'd be in this list.
      tasks.map { it.path } shouldBe listOf(":moduleCheck")
    }
  }

  @Test
  fun `buildConfig task should not be required when disabled in android test module`() {
    androidLibrary(":", "com.modulecheck.lib1") {
      buildFile {
        """
        plugins {
          id("com.android.test") version "$DEFAULT_AGP_VERSION"
          kotlin("android") version "$DEFAULT_KOTLIN_VERSION"
          id("com.rickbusarow.module-check")
        }
        allprojects {
          repositories {
            mavenCentral()
            google()
            jcenter()
            maven("https://plugins.gradle.org/m2/")
            maven("https://oss.sonatype.org/content/repositories/snapshots")
          }
        }

        android {
          targetProjectPath = ":app"
          defaultConfig {
            minSdkVersion(23)
            compileSdkVersion(30)
            targetSdkVersion(30)
          }
          buildFeatures.buildConfig = false
        }

        moduleCheck {
          // this is a compileOnly dependency for test modules
          ignoreUnusedFinding = setOf(":app")
        }
        """
      }

      projectDir.child("settings.gradle.kts").createSafely(
        """
        include(":app")
        """.trimIndent()
      )
    }

    androidLibrary(":app", "com.modulecheck.app") {
      buildFile {
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android {
          defaultConfig {
            minSdkVersion(23)
            compileSdkVersion(30)
            targetSdkVersion(30)
          }
          buildFeatures.buildConfig = false
        }
        """
      }
    }

    shouldSucceed("moduleCheck").apply {
      // Assert that nothing else executed.
      // If ModuleCheck were relying upon buildConfig tasks, they'd be in this list.
      tasks.map { it.path } shouldBe listOf(":moduleCheck")
    }
  }
}
