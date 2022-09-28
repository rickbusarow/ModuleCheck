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

import modulecheck.gradle.platforms.Classpath
import modulecheck.model.sourceset.SourceSetName
import modulecheck.model.sourceset.asSourceSetName
import modulecheck.project.McProject
import modulecheck.utils.remove
import org.junit.jupiter.api.TestFactory
import java.io.File

class ClasspathResolutionTest : BaseGradleTest() {

  @TestFactory
  fun `kotlin library with external dependency`() = factory {
    val lib = kotlinProject(":lib") {
      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        println()

        dependencies {
          implementation("com.google.auto:auto-common:1.0.1")
        }
        """
      }
    }

    shouldSucceed("moduleCheck")

    lib.classpathFileText(SourceSetName.MAIN) shouldBe """
      com.google.auto/auto-common/1.0.1/auto-common-1.0.1.jar
      org.jetbrains.kotlin/kotlin-stdlib-jdk8/$kotlinVersion/kotlin-stdlib-jdk8-$kotlinVersion.jar
      """

    lib.classpathFileText(SourceSetName.TEST) shouldBe """
      com.google.auto/auto-common/1.0.1/auto-common-1.0.1.jar
      org.jetbrains.kotlin/kotlin-stdlib-jdk8/$kotlinVersion/kotlin-stdlib-jdk8-$kotlinVersion.jar
      """
  }

  @TestFactory
  fun `android library with external dependency`() = factory {
    val lib = androidLibrary(":lib", "com.modulecheck.lib1") {
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
          implementation("com.google.auto:auto-common:1.0.1")
        }
        """
      }
    }

    shouldSucceed("moduleCheck").apply {
      // Assert that nothing else executed.
      // If ModuleCheck is relying upon buildConfig tasks, they'll be in this list.
      // tasks.map { it.path }.sorted() shouldContainAll listOf(
      //           ":lib:generateDebugAndroidTestBuildConfig",
      //           ":lib:generateDebugBuildConfig",
      //           ":lib:generateReleaseBuildConfig",
      //           ":moduleCheck"
      //         )
      tasks.map { it.path }.sorted().joinToString("\n") shouldBe ""
    }

    lib.classpathFileText(SourceSetName.MAIN) shouldBe """
        com.google.auto/auto-common/1.0.1/auto-common-1.0.1.jar
        org.jetbrains.kotlin/kotlin-stdlib-jdk8/$kotlinVersion/kotlin-stdlib-jdk8-$kotlinVersion.jar
      """

    lib.classpathFileText(SourceSetName.DEBUG) shouldBe """
        /lib/build/generated/source/buildConfig/debug/com/modulecheck/lib1/BuildConfig.java
        /lib/build/intermediates/aapt_friendly_merged_manifests/debug/aapt/AndroidManifest.xml
        /lib/build/intermediates/aapt_friendly_merged_manifests/debug/aapt/output-metadata.json
        /lib/build/intermediates/compile_r_class_jar/debug/R.jar
        /lib/build/intermediates/compile_symbol_list/debug/R.txt
        /lib/build/intermediates/manifest_merge_blame_file/debug/manifest-merger-blame-debug-report.txt
        /lib/build/intermediates/merged_manifest/debug/AndroidManifest.xml
        /lib/build/intermediates/packaged_manifests/debug/output-metadata.json
        /lib/build/intermediates/symbol_list_with_package_name/debug/package-aware-r.txt
        /lib/build/outputs/logs/manifest-merger-debug-report.txt
        com.google.auto/auto-common/1.0.1/auto-common-1.0.1.jar
        org.jetbrains.kotlin/kotlin-stdlib-jdk8/$kotlinVersion/kotlin-stdlib-jdk8-$kotlinVersion.jar
      """

    lib.classpathFileText(SourceSetName.ANDROID_TEST) shouldBe ""

    lib.classpathFileText("androidTestDebug".asSourceSetName()) shouldBe """
        /lib/build/generated/source/buildConfig/androidTest/debug/com/modulecheck/lib1/test/BuildConfig.java
        /lib/build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/debugAndroidTest/R.jar
        /lib/build/intermediates/manifest_merge_blame_file/debugAndroidTest/manifest-merger-blame-debug-androidTest-report.txt
        /lib/build/intermediates/packaged_manifests/debugAndroidTest/AndroidManifest.xml
        /lib/build/intermediates/packaged_manifests/debugAndroidTest/output-metadata.json
        /lib/build/intermediates/processed_res/debugAndroidTest/out/output-metadata.json
        /lib/build/intermediates/processed_res/debugAndroidTest/out/resources-debugAndroidTest.ap_
        /lib/build/intermediates/runtime_symbol_list/debugAndroidTest/R.txt
        /lib/build/intermediates/symbol_list_with_package_name/debugAndroidTest/package-aware-r.txt
      """
  }

  @TestFactory
  fun `android application with project dependency`() =
    factory {
      val lib = androidLibrary(":lib", "com.modulecheck.lib1") {
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
            implementation("com.google.auto:auto-common:1.0.1")
          }
          """
        }
      }
      val app = androidApplication(":app", "com.modulecheck.app") {

        buildFile {
          """
          plugins {
            id("com.android.application")
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
            implementation(project(":lib"))
          }
          """
        }
      }

      shouldSucceed("moduleCheck").apply {
        // Assert that nothing else executed.
        // If ModuleCheck is relying upon buildConfig tasks, they'll be in this list.
        // tasks.map { it.path }.sorted() shouldContainAll listOf(
        //           ":lib:generateDebugAndroidTestBuildConfig",
        //           ":lib:generateDebugBuildConfig",
        //           ":lib:generateReleaseBuildConfig",
        //           ":moduleCheck"
        //         )
        tasks.map { it.path }.sorted().joinToString("\n") shouldBe ""
      }

      app.classpathFileText(SourceSetName.MAIN) shouldBe """
        com.google.auto/auto-common/1.0.1/auto-common-1.0.1.jar
        org.jetbrains.kotlin/kotlin-stdlib-jdk8/$kotlinVersion/kotlin-stdlib-jdk8-$kotlinVersion.jar
      """

      app.classpathFileText(SourceSetName.DEBUG) shouldBe """
        /lib/build/generated/source/buildConfig/debug/com/modulecheck/lib1/BuildConfig.java
        /lib/build/intermediates/aapt_friendly_merged_manifests/debug/aapt/AndroidManifest.xml
        /lib/build/intermediates/aapt_friendly_merged_manifests/debug/aapt/output-metadata.json
        /lib/build/intermediates/compile_r_class_jar/debug/R.jar
        /lib/build/intermediates/compile_symbol_list/debug/R.txt
        /lib/build/intermediates/manifest_merge_blame_file/debug/manifest-merger-blame-debug-report.txt
        /lib/build/intermediates/merged_manifest/debug/AndroidManifest.xml
        /lib/build/intermediates/packaged_manifests/debug/output-metadata.json
        /lib/build/intermediates/symbol_list_with_package_name/debug/package-aware-r.txt
        /lib/build/outputs/logs/manifest-merger-debug-report.txt
        com.google.auto/auto-common/1.0.1/auto-common-1.0.1.jar
        org.jetbrains.kotlin/kotlin-stdlib-jdk8/$kotlinVersion/kotlin-stdlib-jdk8-$kotlinVersion.jar
      """

      app.classpathFileText(SourceSetName.ANDROID_TEST) shouldBe ""

      app.classpathFileText("androidTestDebug".asSourceSetName()) shouldBe """
        /lib/build/generated/source/buildConfig/androidTest/debug/com/modulecheck/lib1/test/BuildConfig.java
        /lib/build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/debugAndroidTest/R.jar
        /lib/build/intermediates/manifest_merge_blame_file/debugAndroidTest/manifest-merger-blame-debug-androidTest-report.txt
        /lib/build/intermediates/packaged_manifests/debugAndroidTest/AndroidManifest.xml
        /lib/build/intermediates/packaged_manifests/debugAndroidTest/output-metadata.json
        /lib/build/intermediates/processed_res/debugAndroidTest/out/output-metadata.json
        /lib/build/intermediates/processed_res/debugAndroidTest/out/resources-debugAndroidTest.ap_
        /lib/build/intermediates/runtime_symbol_list/debugAndroidTest/R.txt
        /lib/build/intermediates/symbol_list_with_package_name/debugAndroidTest/package-aware-r.txt
      """
    }

  fun McProject.classpathFileText(sourceSetName: SourceSetName): String {

    val testKitM2 = testKitDir.resolve("caches/modules-2/files-2.1")
      .absolutePath.replace('/', File.separatorChar)

    val startPartLength = testKitM2.split(File.separatorChar).size

    fun String.clean(): String {
      return if (!startsWith(testKitM2)) {
        remove(testProjectDir.absolutePath)
      } else {
        split(File.separatorChar).drop(startPartLength)
          .let { segments -> segments.dropLast(2) + segments.last() }
          .joinToString("/")
      }
    }

    return Classpath.from(this, sourceSetName).files
      .joinToString("\n") { it.absolutePath.clean() }
  }
}
