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

import modulecheck.config.fake.TestChecksSettings
import modulecheck.config.fake.TestSettings
import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.gradle.model.asConfigurationName
import modulecheck.runtime.test.ProjectFindingReport.disableViewBinding
import modulecheck.runtime.test.RunnerTest
import modulecheck.utils.child
import modulecheck.utils.createSafely
import org.junit.jupiter.api.Test

class DisableViewBindingTest : RunnerTest() {

  override val settings by resets { TestSettings(checks = TestChecksSettings(disableViewBinding = true)) }

  @Test
  fun `used ViewBinding from main sourceset in dependent module with no changes`() {

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
      addLayoutFile(
        "fragment_lib1.xml",
        """<?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout
          xmlns:android="https://schemas.android.com/apk/res/android"
          android:id="@+id/fragment_container"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          />
        """
      )
    }

    androidLibrary(":lib2", "com.modulecheck.lib2") {
      addDependency(ConfigurationName.api, lib1)
      platformPlugin.viewBindingEnabled = false

      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.databinding.FragmentLib1Binding

        val binding = FragmentLib1Binding()
        """
      )
    }

    run(
      autoCorrect = false
    ).isSuccess shouldBe true

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
  fun `used ViewBinding from debug sourceset in dependent module with no changes`() {

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
      addLayoutFile(
        "fragment_lib1.xml",
        """<?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout
          xmlns:android="https://schemas.android.com/apk/res/android"
          android:id="@+id/fragment_container"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          />
        """,
        SourceSetName.DEBUG
      )
    }

    androidLibrary(":lib2", "com.modulecheck.lib2") {
      addDependency("debugImplementation".asConfigurationName(), lib1)
      platformPlugin.viewBindingEnabled = false

      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.databinding.FragmentLib1Binding

        private val binding = FragmentLib1Binding()
        """,
        SourceSetName.DEBUG
      )
    }

    run(
      autoCorrect = false
    ).isSuccess shouldBe true

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
  fun `used ViewBinding in contributing module`() {

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
      addLayoutFile(
        "fragment_lib1.xml",
        """<?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout
          xmlns:android="https://schemas.android.com/apk/res/android"
          android:id="@+id/fragment_container"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          />
        """
      )

      addKotlinSource(
        """
        package com.modulecheck.lib1

        import com.modulecheck.lib1.databinding.FragmentLib1Binding

        val binding = FragmentLib1Binding()
        """
      )
    }

    run(
      autoCorrect = false
    ).isSuccess shouldBe true

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
  fun `ViewBinding from main is used in debug source set`() {

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
      addLayoutFile(
        "fragment_lib1.xml",
        """<?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout
          xmlns:android="https://schemas.android.com/apk/res/android"
          android:id="@+id/fragment_container"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          />
        """
      )

      // Setting a debug base package, but it's never used.  The inferred FqName for the generated
      // binding should still reflect the main source set even though it's evaluating a file in
      // debug.
      platformPlugin.manifests[SourceSetName.DEBUG] = projectDir
        .child("src/debug/AndroidManifest.xml")
        .createSafely("<manifest package=\"com.modulecheck.lib1.debug\" />")

      addKotlinSource(
        """
        package com.modulecheck.lib1

        import com.modulecheck.lib1.databinding.FragmentLib1Binding

        val binding = FragmentLib1Binding()
        """,
        SourceSetName.DEBUG
      )
    }

    run(
      autoCorrect = false
    ).isSuccess shouldBe true

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
  fun `ViewBinding from debug & release is used in main source set`() {

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
      addLayoutFile(
        "fragment_lib1.xml",
        """<?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout
          xmlns:android="https://schemas.android.com/apk/res/android"
          android:id="@+id/fragment_container"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          />
        """,
        SourceSetName.DEBUG
      )
      addLayoutFile(
        "fragment_lib1.xml",
        """<?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout
          xmlns:android="https://schemas.android.com/apk/res/android"
          android:id="@+id/fragment_container"
          android:layout_width="match_parent"
          android:layout_height="match_parent">

          <!-- Slightly different XML layout for release-->
        </androidx.constraintlayout.widget.ConstraintLayout>
        """,
        SourceSetName.RELEASE
      )

      // Setting a debug base package, but it's never used.  The inferred FqName for the generated
      // binding should still reflect the main source set even though it's evaluating a file in
      // debug.
      platformPlugin.manifests[SourceSetName.DEBUG] = projectDir
        .child("src/debug/AndroidManifest.xml")
        .createSafely("<manifest package=\"com.modulecheck.lib1.debug\" />")

      addKotlinSource(
        """
        package com.modulecheck.lib1

        import com.modulecheck.lib1.databinding.FragmentLib1Binding

        val binding = FragmentLib1Binding()
        """
      )
    }

    run(
      autoCorrect = false
    ).isSuccess shouldBe true

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
  fun `ViewBinding from debug with different base package is used in debug source set`() {

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
      addLayoutFile(
        "fragment_lib1.xml",
        """<?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout
          xmlns:android="https://schemas.android.com/apk/res/android"
          android:id="@+id/fragment_container"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          />
        """,
        SourceSetName.DEBUG
      )

      platformPlugin.manifests[SourceSetName.DEBUG] = projectDir
        .child("src/debug/AndroidManifest.xml")
        .createSafely("<manifest package=\"com.modulecheck.lib1.debug\" />")

      addKotlinSource(
        """
        package com.modulecheck.lib1.debug

        import com.modulecheck.lib1.debug.databinding.FragmentLib1Binding

        val binding = FragmentLib1Binding()
        """,
        SourceSetName.DEBUG
      )
    }

    run(
      autoCorrect = false
    ).isSuccess shouldBe true

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
  fun `ViewBinding from debug without different base package is used in debug source set`() {

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
      addLayoutFile(
        "fragment_lib1.xml",
        """<?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout
          xmlns:android="https://schemas.android.com/apk/res/android"
          android:id="@+id/fragment_container"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          />
        """,
        SourceSetName.DEBUG
      )

      // The layout file is in debug, so it's generated in debug.  But it doesn't have a custom
      // package for debug, so it should be generated using the base package from main.

      addKotlinSource(
        """
        package com.modulecheck.lib1.debug

        import com.modulecheck.lib1.databinding.FragmentLib1Binding

        val binding = FragmentLib1Binding()
        """,
        SourceSetName.DEBUG
      )
    }

    run(
      autoCorrect = false
    ).isSuccess shouldBe true

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
  fun `unused ViewBinding should pass if check is disabled`() {

    settings.checks.disableViewBinding = false

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
      addLayoutFile(
        "fragment_lib1.xml",
        """<?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout
          xmlns:android="https://schemas.android.com/apk/res/android"
          android:id="@+id/fragment_container"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          />
        """
      )
    }

    run(
      autoCorrect = false
    ).isSuccess shouldBe true

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
  fun `unused ViewBinding should pass if suppressed`() {

    val lib1 = androidLibrary(":lib1", "com.modulecheck.lib1") {
      buildFile {
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android {
          @Suppress("disable-view-binding")
          buildFeatures.viewBinding = true
        }
        """
      }
    }

    run(
      autoCorrect = false
    ).isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        id("com.android.library")
        kotlin("android")
      }

      android {
        @Suppress("disable-view-binding")
        buildFeatures.viewBinding = true
      }
    """

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `unused ViewBinding without auto-correct should fail`() {

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
      addLayoutFile(
        "fragment_lib1.xml",
        """<?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout
          xmlns:android="https://schemas.android.com/apk/res/android"
          android:id="@+id/fragment_container"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          />
        """
      )
    }

    run(
      autoCorrect = false
    ).isSuccess shouldBe false

    lib1.buildFile shouldHaveText """
      plugins {
        id("com.android.library")
        kotlin("android")
      }

      android {
        buildFeatures.viewBinding = true
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(disableViewBinding(false, "7, 3"))
    )
  }

  @Test
  fun `unused ViewBinding when scoped and then qualified should be fixed`() {

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
    }

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        id("com.android.library")
        kotlin("android")
      }

      android {
        buildFeatures.viewBinding = false
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(disableViewBinding(true, "7, 3"))
    )
  }

  @Test
  fun `unused ViewBinding without buildFeatures block should be fixed`() {

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
        buildFeatures.viewBinding = false
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(disableViewBinding(true, null))
    )
  }

  @Test
  fun `unused ViewBinding without android block should add android block under existing plugins block`() {

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
          viewBinding = false
        }
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(disableViewBinding(true, null))
    )
  }

  @Test
  fun `unused ViewBinding without android or plugins block should add android block above dependencies block`() {

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
          viewBinding = false
        }
      }

      dependencies {
      }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(disableViewBinding(true, null))
    )
  }

  @Test
  fun `unused ViewBinding when fully qualified should be fixed`() {

    val lib1 = androidLibrary(":lib1", "com.modulecheck.lib1") {
      buildFile {
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android.buildFeatures.viewBinding = true
        """
      }
      addLayoutFile(
        "fragment_lib1.xml",
        """<?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout
          xmlns:android="https://schemas.android.com/apk/res/android"
          android:id="@+id/fragment_container"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          />
        """
      )
    }

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
      plugins {
        id("com.android.library")
        kotlin("android")
      }

      android.buildFeatures.viewBinding = false
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(disableViewBinding(true, "6, 1"))
    )
  }

  @Test
  fun `unused ViewBinding when fully scoped should be fixed`() {

    val lib1 = androidLibrary(":lib1", "com.modulecheck.lib1") {
      buildFile {
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
      }
      addLayoutFile(
        "fragment_lib1.xml",
        """<?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout
          xmlns:android="https://schemas.android.com/apk/res/android"
          android:id="@+id/fragment_container"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          />
        """
      )
    }

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android {
          buildFeatures {
            viewBinding = false
          }
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(disableViewBinding(true, "8, 5"))
    )
  }

  @Test
  fun `unused ViewBinding when qualified and then scoped should be fixed`() {

    val lib1 = androidLibrary(":lib1", "com.modulecheck.lib1") {
      buildFile {
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android.buildFeatures {
          viewBinding = true
        }
        """
      }
      addLayoutFile(
        "fragment_lib1.xml",
        """<?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout
          xmlns:android="https://schemas.android.com/apk/res/android"
          android:id="@+id/fragment_container"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          />
        """
      )
    }

    run().isSuccess shouldBe true

    lib1.buildFile shouldHaveText """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        android.buildFeatures {
          viewBinding = false
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib1" to listOf(disableViewBinding(true, "7, 3"))
    )
  }
}
