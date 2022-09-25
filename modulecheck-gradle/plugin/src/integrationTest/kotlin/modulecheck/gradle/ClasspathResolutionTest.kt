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

import io.kotest.matchers.collections.shouldContainAll
import org.junit.jupiter.api.TestFactory

class ClasspathResolutionTest : BaseGradleTest() {

  @TestFactory
  fun `exclude with group only in external dependency`() =
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
            minSdkVersion(23)
            compileSdkVersion(30)
            targetSdkVersion(30)
          }
        }
        dependencies {
          implementation("com.google.auto:auto-common:1.0.1") {
            exclude(group = "com.foo")
          }
        }
        """
        }
      }

      shouldSucceed("moduleCheck").apply {
        // Assert that nothing else executed.
        // If ModuleCheck is relying upon buildConfig tasks, they'll be in this list.
        tasks.map { it.path }.sorted() shouldContainAll listOf(
          ":lib:generateDebugAndroidTestBuildConfig",
          ":lib:generateDebugBuildConfig",
          ":lib:generateReleaseBuildConfig",
          ":moduleCheck"
        )
      }
    }

}
