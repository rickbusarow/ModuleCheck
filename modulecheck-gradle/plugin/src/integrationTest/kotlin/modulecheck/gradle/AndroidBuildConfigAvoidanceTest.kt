/*
 * Copyright (C) 2021-2024 Rick Busarow
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

import io.kotest.matchers.collections.shouldNotContainAnyOf
import org.junit.jupiter.api.TestFactory

class AndroidBuildConfigAvoidanceTest : BaseGradleTest() {

  @TestFactory
  fun `buildConfig task not should be required if not explicitly enabled in android library module`() =
    factory {
      androidLibrary(":lib", "com.modulecheck.lib1") {
        buildFile {
          """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android {
          defaultConfig {
            minSdk = 23
            compileSdk = 33
            targetSdk = 33
          }
          namespace = "com.modulecheck.lib1"
        }
        """
        }
      }

      shouldSucceed(":lib:assembleDebug")

      shouldSucceed("moduleCheck").apply {
        // Assert that nothing else executed.
        // If ModuleCheck is relying upon buildConfig tasks, they'll be in this list.
        tasks.map { it.path }.sorted() shouldNotContainAnyOf listOf(
          ":lib:generateDebugAndroidTestBuildConfig",
          ":lib:generateDebugBuildConfig",
          ":lib:generateReleaseBuildConfig"
        )
      }
    }

  @TestFactory
  fun `buildConfig task should not be required when disabled in android library module`() =
    factory {
      androidLibrary(":lib", "com.modulecheck.lib1") {
        buildFile {
          """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android {
          defaultConfig {
            minSdk = 23
            compileSdk = 33
            targetSdk = 33
          }
          buildFeatures.buildConfig = false
          namespace = "com.modulecheck.lib1"
        }
        """
        }
      }

      shouldSucceed("moduleCheck").apply {
        // Assert that nothing else executed.
        // If ModuleCheck were relying upon buildConfig tasks, they'd be in this list.
        tasks.map { it.path } shouldNotContainAnyOf listOf(
          ":lib:generateDebugAndroidTestBuildConfig",
          ":lib:generateDebugBuildConfig",
          ":lib:generateReleaseBuildConfig"
        )
      }
    }
}
