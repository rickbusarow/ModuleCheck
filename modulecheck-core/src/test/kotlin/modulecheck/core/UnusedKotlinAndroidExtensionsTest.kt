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
import modulecheck.project.test.AndroidMcProjectBuilderScope
import modulecheck.runtime.test.ProjectFindingReport.unusedKotlinAndroidExtensions
import modulecheck.runtime.test.RunnerTest
import modulecheck.testing.writeKotlin
import org.junit.jupiter.api.Test

class UnusedKotlinAndroidExtensionsTest : RunnerTest() {

  override val settings by resets {
    TestSettings(
      checks = TestChecksSettings(
        unusedKotlinAndroidExtensions = true
      )
    )
  }

  @Test
  fun `unused KotlinAndroidExtensions should pass if check is disabled`() {
    settings.checks.unusedKotlinAndroidExtensions = false

    androidProject(":lib1", "com.modulecheck.lib1") {
      writeBuildFileWithPlugin()
      addAnyLayoutFile()
    }

    run(autoCorrect = false).isSuccess shouldBe true
    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `unused KotlinAndroidExtensions without auto-correct should fail`() {
    val lib1 = androidProject(":lib1", "com.modulecheck.lib1") {
      writeBuildFileWithPlugin()
      addAnyLayoutFile()
    }

    run(autoCorrect = false).isSuccess shouldBe false

    lib1.buildFile.readText() shouldBe """
      plugins {
        id("com.android.library")
        kotlin("android")
        kotlin("android-extensions")
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(unusedKotlinAndroidExtensions(fixed = false, position = "4, 3"))
    )
  }

  @Test
  fun `used KotlinAndroidExtensions should pass and should not be corrected`() {
    val lib1 = androidProject(":lib1", "com.modulecheck.lib1") {
      writeBuildFileWithPlugin()
      addAnyLayoutFile()
      addSource(
        "com/modulecheck/lib1/Source.kt",
        """
        package com.modulecheck.lib1

        import android.app.Activity
        import kotlinx.android.synthetic.main.fragment_lib1.*

        class SampleActivity : Activity {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.fragment_lib1)

                text_view.setText("Some text")
            }
        }
        """
      )
    }

    run(autoCorrect = true).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        id("com.android.library")
        kotlin("android")
        kotlin("android-extensions")
      }
    """

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `unused KotlinAndroidExtensions should be fixed`() {
    val lib1 = androidProject(":lib1", "com.modulecheck.lib1") {
      writeBuildFileWithPlugin()
    }

    run(autoCorrect = true).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        id("com.android.library")
        kotlin("android")
        // kotlin("android-extensions")  // ModuleCheck finding [unusedKotlinAndroidExtensions]
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(unusedKotlinAndroidExtensions(fixed = true, position = "4, 3"))
    )
  }

  private fun AndroidMcProjectBuilderScope.writeBuildFileWithPlugin() {
    buildFile.writeKotlin(
      """
          plugins {
            id("com.android.library")
            kotlin("android")
            kotlin("android-extensions")
          }
      """
    )
  }

  private fun AndroidMcProjectBuilderScope.addAnyLayoutFile() {
    addLayoutFile(
      "fragment_lib1.xml",
      """<?xml version="1.0" encoding="utf-8"?>
          <androidx.constraintlayout.widget.ConstraintLayout
            xmlns:android="https://schemas.android.com/apk/res/android"
            android:id="@+id/fragment_container"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            >

            <TextView
              android:id="@+id/text_view"
              android:layout_width="match_parent"
              android:layout_height="match_parent"
              />

          </androidx.constraintlayout.widget.ConstraintLayout>
      """
    )
  }
}
