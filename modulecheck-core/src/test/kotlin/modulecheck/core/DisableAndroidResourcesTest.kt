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

package modulecheck.core

import modulecheck.api.test.TestChecksSettings
import modulecheck.api.test.TestSettings
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.runtime.test.ProjectFindingReport.disableAndroidResources
import modulecheck.runtime.test.RunnerTest
import org.junit.jupiter.api.Test

class DisableAndroidResourcesTest : RunnerTest() {

  override val settings by resets { TestSettings(checks = TestChecksSettings(disableAndroidResources = true)) }

  @Test
  fun `resource generation is used in contributing module with no changes`() {

    val lib1 = androidLibrary(":lib1", "com.modulecheck.lib1") {
      buildFile {
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }
        """
      }
      addResourceFile(
        "values/strings.xml",
        """<resources>
            |  <string name="app_name" translatable="false">MyApp</string>
            |</resources>
        """.trimMargin()
      )
      addKotlinSource(
        """
        package com.modulecheck.lib1

        val string = R.string.app_name
        """
      )
    }

    run(autoCorrect = false).isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        id("com.android.library")
        kotlin("android")
      }
    """

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `resource generation is used in dependent module with no changes`() {

    val lib1 = androidLibrary(":lib1", "com.modulecheck.lib1") {
      buildFile {
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android {
          buildFeatures.viewBinding = true
        }
        """
      }
      addResourceFile(
        "values/strings.xml",
        """<resources>
            |  <string name="app_name" translatable="false">MyApp</string>
            |</resources>
        """.trimMargin()
      )
    }

    androidLibrary(":lib2", "com.modulecheck.lib2") {
      addDependency(ConfigurationName.api, lib1)
      platformPlugin.androidResourcesEnabled = false

      addKotlinSource(
        """
        package com.modulecheck.lib2

        val name = R.string.app_name
        """
      )
    }

    run(autoCorrect = false).isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        id("com.android.library")
        kotlin("android")
      }

      android {
        buildFeatures.viewBinding = true
      }
    """

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `unused resource generation without autocorrect should fail and be reported`() {

    val lib1 = androidLibrary(":lib1", "com.modulecheck.lib1") {
      buildFile {
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }
        """
      }
    }

    run(autoCorrect = false).isSuccess shouldBe false

    lib1.buildFile shouldHaveText """
    plugins {
      id("com.android.library")
      kotlin("android")
    }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(
        disableAndroidResources(false, null)
      )
    )
  }

  @Test
  fun `unused resource generation when scoped and then qualified should be fixed`() {

    val lib1 = androidLibrary(":lib1", "com.modulecheck.lib1") {
      buildFile {
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }
        android {
          buildFeatures.androidResources = true
        }
        """
      }
    }

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        id("com.android.library")
        kotlin("android")
      }
      android {
        buildFeatures.androidResources = false
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(
        disableAndroidResources(true, "6, 3")
      )
    )
  }

  @Test
  fun `unused resource generation without buildFeatures block should be fixed`() {

    val lib1 = androidLibrary(":lib1", "com.modulecheck.lib1") {
      buildFile {
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android {
          mindSdk(21)
        }
        """
      }
    }

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        id("com.android.library")
        kotlin("android")
      }

      android {
        mindSdk(21)
        buildFeatures.androidResources = false
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(
        disableAndroidResources(true, null)
      )
    )
  }

  @Test
  fun `unused resource generation without android block should add android block under existing plugins block`() {

    val lib1 = androidLibrary(":lib1", "com.modulecheck.lib1") {
      buildFile {
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }
        """
      }
    }

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        id("com.android.library")
        kotlin("android")
      }

      android {
        buildFeatures {
          androidResources = false
        }
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(
        disableAndroidResources(true, null)
      )
    )
  }

  @Test
  fun `unused resource generation without android or plugins block should add android block above dependencies block`() {

    val lib1 = androidLibrary(":lib1", "com.modulecheck.lib1") {
      buildFile {
        """
        apply(plugin = "com.android.library")
        apply(plugin = "org.jetbrains.kotlin-android")

        dependencies {
        }
        """
      }
    }

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      apply(plugin = "com.android.library")
      apply(plugin = "org.jetbrains.kotlin-android")

      android {
        buildFeatures {
          androidResources = false
        }
      }

      dependencies {
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(
        disableAndroidResources(true, null)
      )
    )
  }

  @Test
  fun `unused resource generation when fully qualified should be fixed`() {

    val lib1 = androidLibrary(":lib1", "com.modulecheck.lib1") {
      buildFile {
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }
        android.buildFeatures.androidResources = true
        """
      }
    }

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        id("com.android.library")
        kotlin("android")
      }
      android.buildFeatures.androidResources = false
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(
        disableAndroidResources(true, "5, 1")
      )
    )
  }

  @Test
  fun `unused resource generation when qualified and then scoped should be fixed`() {

    val lib1 = androidLibrary(":lib1", "com.modulecheck.lib1") {
      buildFile {
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }
        android.buildFeatures {
          androidResources = true
        }
        """
      }
    }

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        id("com.android.library")
        kotlin("android")
      }
      android.buildFeatures {
        androidResources = false
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(
        disableAndroidResources(true, "6, 3")
      )
    )
  }

  @Test
  fun `unused resource generation when fully scoped should be fixed`() {

    val lib1 = androidLibrary(":lib1", "com.modulecheck.lib1") {
      buildFile {
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }
        android {
          buildFeatures {
            androidResources = true
          }
        }
        """
      }
    }

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
        plugins {
          id("com.android.library")
          kotlin("android")
        }
        android {
          buildFeatures {
            androidResources = false
          }
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(
        disableAndroidResources(true, "7, 5")
      )
    )
  }

  @Test
  fun `unused resource generation with autocorrect and no explicit buildFeatures property should be fixed`() {

    val lib1 = androidLibrary(":lib1", "com.modulecheck.lib1") {
      buildFile {
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }
        """
      }
    }

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        id("com.android.library")
        kotlin("android")
      }

      android {
        buildFeatures {
          androidResources = false
        }
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(
        disableAndroidResources(true, null)
      )
    )
  }

  @Test
  fun `unused resource generation with autocorrect and no android block should be fixed`() {

    val lib1 = androidLibrary(":lib1", "com.modulecheck.lib1") {
      buildFile {
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }
        """
      }
    }

    run().isSuccess shouldBe true

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(
        disableAndroidResources(true, null)
      )
    )

    lib1.buildFile shouldHaveText """
      plugins {
        id("com.android.library")
        kotlin("android")
      }

      android {
        buildFeatures {
          androidResources = false
        }
      }
    """
  }
}
