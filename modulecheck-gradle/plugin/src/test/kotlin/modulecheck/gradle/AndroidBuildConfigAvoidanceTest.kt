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

import org.junit.jupiter.api.TestFactory

class AndroidBuildConfigAvoidanceTest : BaseGradleTest() {

  @TestFactory
  fun `buildConfig task should be required when not configured in android library module`() =
    dynamic {
      androidLibrary(":lib", "com.modulecheck.lib1") {
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
        }
        """
        }
      }

      shouldSucceed("moduleCheck").apply {
        // Assert that nothing else executed.
        // If ModuleCheck were relying upon buildConfig tasks, they'd be in this list.
        tasks.map { it.path }.sorted() shouldBe listOf(
          ":lib:extractDeepLinksDebug",
          ":lib:generateDebugAndroidTestBuildConfig",
          ":lib:generateDebugBuildConfig",
          ":lib:generateReleaseBuildConfig",
          ":lib:preBuild",
          ":lib:preDebugAndroidTestBuild",
          ":lib:preDebugBuild",
          ":lib:preReleaseBuild",
          ":lib:processDebugAndroidTestManifest",
          ":lib:processDebugManifest",
          ":moduleCheck"
        )
      }
    }

  @TestFactory
  fun `buildConfig task should not be required when disabled in android library module`() =
    dynamic {
      androidLibrary(":lib", "com.modulecheck.lib1") {
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

  @TestFactory
  fun `buildConfig task should not be required when disabled in android dynamic-feature module`() =
    dynamic {
      androidLibrary(":lib", "com.modulecheck.lib1") {
        buildFile {
          """
        plugins {
          id("com.android.dynamic-feature")
          kotlin("android")
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
      }

      shouldSucceed("moduleCheck").apply {
        // Assert that nothing else executed.
        // If ModuleCheck were relying upon buildConfig tasks, they'd be in this list.
        tasks.map { it.path } shouldBe listOf(":moduleCheck")
      }
    }

  @TestFactory
  fun `buildConfig task should not be required when disabled in android test module`() = dynamic {
    androidLibrary(":lib", "com.modulecheck.lib1") {
      buildFile {
        """
        plugins {
          id("com.android.test")
          kotlin("android")
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

        dependencies {
          @Suppress("unused-dependency")
          compileOnly(project(path = ":app"))
        }
        """
      }
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
