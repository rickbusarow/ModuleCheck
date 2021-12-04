/*
 * Copyright (C) 2021 Rick Busarow
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
import modulecheck.core.rule.ModuleCheckRuleFactory
import modulecheck.core.rule.MultiRuleFindingFactory
import modulecheck.project.ConfigurationName
import modulecheck.project.test.writeKotlin
import modulecheck.runtime.test.RunnerTest
import org.junit.jupiter.api.Test

class DisableViewBindingTest : RunnerTest() {

  val ruleFactory by resets { ModuleCheckRuleFactory() }

  override val settings by resets { TestSettings(checks = TestChecksSettings(disableViewBinding = true)) }
  val findingFactory by resets {
    MultiRuleFindingFactory(
      settings,
      ruleFactory.create(settings)
    )
  }

  @Test
  fun `used ViewBinding in dependent module with no changes`() {

    val runner = runner(
      autoCorrect = false,
      findingFactory = findingFactory
    )

    val lib1 = androidProject(":lib1", "com.modulecheck.lib1") {
      buildFile.writeKotlin(
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android {
          buildFeatures.viewBinding = true
        }
        """
      )
      addLayoutFile(
        "fragment_lib1.xml",
        """<?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout
          xmlns:android="https://schemas.android.com/apk/res/android"
          android:id="@+id/fragment_container"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          >

          <com.modulecheck.lib1.Lib1View
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            />

        </androidx.constraintlayout.widget.ConstraintLayout>"""
      )
    }

    androidProject(":lib2", "com.modulecheck.lib2") {
      addDependency(ConfigurationName.api, lib1)
      viewBindingEnabled = false

      addSource(
        "com/modulecheck/lib2/Source.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.databinding.FragmentLib1Binding

        val binding = FragmentLib1Binding()
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        id("com.android.library")
        kotlin("android")
      }

      android {
        buildFeatures.viewBinding = true
      }"""

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `used ViewBinding in contributing module`() {

    val runner = runner(
      autoCorrect = false,
      findingFactory = findingFactory
    )

    val lib1 = androidProject(":lib1", "com.modulecheck.lib1") {
      buildFile.writeKotlin(
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android {
          buildFeatures.viewBinding = true
        }
        """
      )
      addLayoutFile(
        "fragment_lib1.xml",
        """<?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout
          xmlns:android="https://schemas.android.com/apk/res/android"
          android:id="@+id/fragment_container"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          >

          <com.modulecheck.lib1.Lib1View
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            />

        </androidx.constraintlayout.widget.ConstraintLayout>"""
      )

      addSource(
        "com/modulecheck/lib1/Source.kt",
        """
        package com.modulecheck.lib1

        import com.modulecheck.lib1.databinding.FragmentLib1Binding

        val binding = FragmentLib1Binding()
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        id("com.android.library")
        kotlin("android")
      }

      android {
        buildFeatures.viewBinding = true
      }"""

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `unused ViewBinding should pass if check is disabled`() {

    settings.checks.disableViewBinding = false

    val runner = runner(
      autoCorrect = false,
      findingFactory = findingFactory
    )

    val lib1 = androidProject(":lib1", "com.modulecheck.lib1") {
      buildFile.writeKotlin(
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android {
          buildFeatures.viewBinding = true
        }
        """
      )
      addLayoutFile(
        "fragment_lib1.xml",
        """<?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout
          xmlns:android="https://schemas.android.com/apk/res/android"
          android:id="@+id/fragment_container"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          >

          <com.modulecheck.lib1.Lib1View
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            />

        </androidx.constraintlayout.widget.ConstraintLayout>"""
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        id("com.android.library")
        kotlin("android")
      }

      android {
        buildFeatures.viewBinding = true
      }"""

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `unused ViewBinding without auto-correct should fail`() {

    val runner = runner(
      autoCorrect = false,
      findingFactory = findingFactory
    )

    val lib1 = androidProject(":lib1", "com.modulecheck.lib1") {
      buildFile.writeKotlin(
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android {
          buildFeatures.viewBinding = true
        }
        """
      )
      addLayoutFile(
        "fragment_lib1.xml",
        """<?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout
          xmlns:android="https://schemas.android.com/apk/res/android"
          android:id="@+id/fragment_container"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          >

          <com.modulecheck.lib1.Lib1View
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            />

        </androidx.constraintlayout.widget.ConstraintLayout>"""
      )
    }

    runner.run(allProjects()).isSuccess shouldBe false

    lib1.buildFile.readText() shouldBe """
      plugins {
        id("com.android.library")
        kotlin("android")
      }

      android {
        buildFeatures.viewBinding = true
      }"""

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib1
                   dependency    name                  source    build file
                X                disableViewBinding              /lib1/build.gradle.kts: (7, 3):

        ModuleCheck found 1 issue
    """
  }

  @Test
  fun `unused ViewBinding when scoped and then qualified should be fixed`() {

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = androidProject(":lib1", "com.modulecheck.lib1") {
      buildFile.writeKotlin(
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android {
          buildFeatures.viewBinding = true
        }
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        id("com.android.library")
        kotlin("android")
      }

      android {
        buildFeatures.viewBinding = false
      }"""

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
              :lib1
                     dependency    name                  source    build file
                  ✔                disableViewBinding              /lib1/build.gradle.kts: (7, 3):

          ModuleCheck found 1 issue
          """
  }

  @Test
  fun `unused ViewBinding when fully qualified should be fixed`() {

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = androidProject(":lib1", "com.modulecheck.lib1") {
      buildFile.writeKotlin(
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android.buildFeatures.viewBinding = true
        """
      )
      addLayoutFile(
        "fragment_lib1.xml",
        """<?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout
          xmlns:android="https://schemas.android.com/apk/res/android"
          android:id="@+id/fragment_container"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          >

          <com.modulecheck.lib1.Lib1View
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            />

        </androidx.constraintlayout.widget.ConstraintLayout>"""
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        id("com.android.library")
        kotlin("android")
      }

      android.buildFeatures.viewBinding = false"""

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib1
                   dependency    name                  source    build file
                ✔                disableViewBinding              /lib1/build.gradle.kts: (6, 1):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `unused ViewBinding when fully scoped should be fixed`() {

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = androidProject(":lib1", "com.modulecheck.lib1") {
      buildFile.writeKotlin(
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android {
          buildFeatures {
            viewBinding = true
          }
        }
        """
      )
      addLayoutFile(
        "fragment_lib1.xml",
        """<?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout
          xmlns:android="https://schemas.android.com/apk/res/android"
          android:id="@+id/fragment_container"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          >

          <com.modulecheck.lib1.Lib1View
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            />

        </androidx.constraintlayout.widget.ConstraintLayout>"""
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android {
          buildFeatures {
            viewBinding = false
          }
        }"""

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib1
                   dependency    name                  source    build file
                ✔                disableViewBinding              /lib1/build.gradle.kts: (8, 5):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `unused ViewBinding when qualified and then scoped should be fixed`() {

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = androidProject(":lib1", "com.modulecheck.lib1") {
      buildFile.writeKotlin(
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android.buildFeatures {
          viewBinding = true
        }
        """
      )
      addLayoutFile(
        "fragment_lib1.xml",
        """<?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout
          xmlns:android="https://schemas.android.com/apk/res/android"
          android:id="@+id/fragment_container"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          >

          <com.modulecheck.lib1.Lib1View
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            />

        </androidx.constraintlayout.widget.ConstraintLayout>"""
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android.buildFeatures {
          viewBinding = false
        }"""

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib1
                   dependency    name                  source    build file
                ✔                disableViewBinding              /lib1/build.gradle.kts: (7, 3):

        ModuleCheck found 1 issue
        """
  }
}
