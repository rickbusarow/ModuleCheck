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

import modulecheck.model.sourceset.SourceSetName
import modulecheck.utils.child
import org.junit.jupiter.api.TestFactory
import java.io.File

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
          defaultConfig {
            minSdkVersion(23)
            compileSdkVersion(30)
            targetSdkVersion(30)
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
          defaultConfig {
            minSdkVersion(23)
            compileSdkVersion(30)
            targetSdkVersion(30)
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
  fun `module with an auto-generated manifest used in subject module should not be unused`() =
    factory {

      // This module is declaring a base package in an auto-generated manifest which isn't present
      // until the manifest processor task is invoked.  That base package needs to be read from the
      // manifest in order to figure out that R declaration.  The "app" module above is referencing
      // that generated R file, so if the manifest isn't generated, the R won't resolve and this
      // module will be unused.
      val lib1 = androidLibrary(":lib1", "com.modulecheck.lib1") {
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

        // This reproduces the behavior of Auto-Manifest:
        // https://github.com/GradleUp/auto-manifest
        // For some reason, that plugin doesn't work with Gradle TestKit.  Its task is never
        // registered, and the manifest location is never changed from the default.  When I open
        // the generated project dir and execute the task from terminal, it works fine...
        // This does the same thing, but uses a different default directory.
        val manifestFile = file("${'$'}buildDir/generated/my-custom-manifest-location/AndroidManifest.xml")

        android.sourceSets {
            findByName("main")?.manifest {
              srcFile(manifestFile.path)
            }
          }
        val makeFile by tasks.registering {

          doFirst {

            manifestFile.parentFile.mkdirs()
            manifestFile.writeText(
              ""${'"'}<manifest package="com.modulecheck.lib1" /> ""${'"'}.trimMargin()
            )
          }
        }

        afterEvaluate {

          tasks.withType(com.android.build.gradle.tasks.GenerateBuildConfig::class.java)
            .configureEach { dependsOn(makeFile) }
          tasks.withType(com.android.build.gradle.tasks.MergeResources::class.java)
            .configureEach { dependsOn(makeFile) }
          tasks.withType(com.android.build.gradle.tasks.ManifestProcessorTask::class.java)
            .configureEach { dependsOn(makeFile)}

        }
        """
        }
      }

      // the manifest is automatically created, so go ahead and delete it for this one test.
      lib1.projectDir.child("src/main/AndroidManifest.xml").delete()

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
        }

        dependencies {
          api(project(path = ":lib1"))
        }
        """
        }

        addKotlinSource(
          """
          package com.modulecheck.app

          val someR = com.modulecheck.lib1.R
          """
        )
      }

      shouldSucceed("moduleCheck")

      // one last check to make sure the manifest wasn't generated, since that would invalidate the test
      File(testProjectDir, "/lib1/src/main/AndroidManifest.xml").exists() shouldBe false
    }

  @TestFactory
  fun `android test fixtures from android DSL should be treated as test fixtures`() = factory(
    filter = { it.agp >= "7.1.0" }
  ) {

    androidLibrary(":lib1", "com.modulecheck.lib1") {
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
          defaultConfig {
            minSdkVersion(23)
            compileSdkVersion(30)
            targetSdkVersion(30)
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
          defaultConfig {
            resValue("string", "app_name", "AppName")

            minSdkVersion(23)
            compileSdkVersion(30)
            targetSdkVersion(30)
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
          defaultConfig {
            minSdkVersion(23)
            compileSdkVersion(30)
            targetSdkVersion(30)
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
