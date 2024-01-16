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

import modulecheck.model.sourceset.SourceSetName
import org.junit.jupiter.api.TestFactory

class UnusedDependenciesPluginTest : BaseGradleTest() {

  @TestFactory
  fun `module with a declaration used in an android module with kotlin source directory should not be unused`() =
    factory {

      androidLibrary(":lib1", "com.modulecheck.lib1") {
        buildFile {
          """
          plugins {
            id("com.android.library")
            kotlin("android")
          }

          android {
            namespace = "com.modulecheck.lib1"
            defaultConfig {
              minSdk = 23
              compileSdk = 32
            }
          }
          """
        }

        addKotlinSource(
          """
          package com.modulecheck.lib1

          class Lib1Class
          """,
          sourceDirName = "kotlin"
        )
      }

      androidLibrary(":lib2", "com.modulecheck.lib2") {
        buildFile {
          """
          plugins {
            id("com.android.library")
            kotlin("android")
          }

          android {
            namespace = "com.modulecheck.lib2"
            defaultConfig {
              minSdk = 23
              compileSdk = 32
            }
          }

          dependencies {
            api(project(path = ":lib1"))
          }
          """
        }

        addKotlinSource(
          """
          package com.modulecheck.lib2

          import com.modulecheck.lib1.Lib1Class

          val lib1Class = Lib1Class()
          """
        )
      }

      shouldSucceed("moduleCheck")
    }

  @TestFactory
  fun `android test fixtures from android DSL should be treated as test fixtures`() = factory(
    exhaustive = true
  ) {

    androidLibrary(":lib1", "com.modulecheck.lib1") {
      buildFile {
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android {
          namespace = "com.modulecheck.lib1"
          defaultConfig {
            minSdk = 23
            compileSdk = 32
          }
          testFixtures.enable = true
        }
        """
      }

      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """,
        SourceSetName.TEST_FIXTURES
      )
    }

    androidLibrary(":lib2", "com.modulecheck.lib2") {
      buildFile {
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android {
          namespace = "com.modulecheck.lib2"
          defaultConfig {
            minSdk = 23
            compileSdk = 32
          }
        }

        dependencies {
          api(testFixtures(project(path = ":lib1")))
        }
        """
      }

      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        val lib1Class = Lib1Class()
        """
      )
    }

    shouldSucceed("moduleCheck")
  }

  @TestFactory
  fun `module with generated string resource used in subject module should not be unused`() =
    factory {

      androidLibrary(":lib1", "com.modulecheck.lib1") {
        buildFile {
          """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android {
          namespace = "com.modulecheck.lib1"
          defaultConfig {
            resValue("string", "app_name", "AppName")

            minSdk = 23
            compileSdk = 32
          }
          buildTypes {
            getByName("debug") {
              resValue("string", "debug_thing", "debug!")
            }
          }
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

      androidLibrary(":lib2", "com.modulecheck.lib2") {
        buildFile {
          """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android {
          namespace = "com.modulecheck.lib2"
          defaultConfig {
            minSdk = 23
            compileSdk = 32
          }
        }

        dependencies {
          implementation(project(path = ":lib1"))
        }
        """
        }

        addKotlinSource(
          """
        package com.modulecheck.lib2

        val lib1Name = R.string.app_name
        """
        )
      }

      shouldSucceed("moduleCheck")
    }
}
