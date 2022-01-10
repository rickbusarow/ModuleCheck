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
import modulecheck.core.rule.ModuleCheckRuleFactory
import modulecheck.core.rule.MultiRuleFindingFactory
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.project.test.writeKotlin
import modulecheck.runtime.test.RunnerTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class DisableAndroidResourcesTest : RunnerTest() {

  val ruleFactory by resets { ModuleCheckRuleFactory() }

  override val settings by resets { TestSettings(checks = TestChecksSettings(disableAndroidResources = true)) }
  val findingFactory by resets {
    MultiRuleFindingFactory(
      settings,
      ruleFactory.create(settings)
    )
  }

  @Test
  fun `resource generation is used in contributing module with no changes`() {

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
        """
      )
      addResourceFile(
        "values/strings.xml",
        """<resources>
            |  <string name="app_name" translatable="false">MyApp</string>
            |</resources>
            """.trimMargin()
      )
      addSource(
        "com/modulecheck/lib1/Source.kt",
        """
        package com.modulecheck.lib1

        val string = R.string.app_name
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """plugins {
  id("com.android.library")
  kotlin("android")
}"""

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `resource generation is used in dependent module with no changes`() {

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
      addResourceFile(
        "values/strings.xml",
        """<resources>
            |  <string name="app_name" translatable="false">MyApp</string>
            |</resources>
            """.trimMargin()
      )
    }

    androidProject(":lib2", "com.modulecheck.lib2") {
      addDependency(ConfigurationName.api, lib1)
      androidResourcesEnabled = false

      addSource(
        "com/modulecheck/lib2/Source.kt",
        """
        package com.modulecheck.lib2

        val name = R.string.app_name
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
  fun `unused resource generation without autocorrect should fail and be reported`() {

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
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe false

    lib1.buildFile.readText() shouldBe """
      plugins {
        id("com.android.library")
        kotlin("android")
      }
      """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib1
                   dependency    name                       source    build file
                X                disableAndroidResources              /lib1/build.gradle.kts:

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `unused resource generation when scoped and then qualified should be fixed`() {

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
          buildFeatures.androidResources = true
        }
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """plugins {
  id("com.android.library")
  kotlin("android")
}
android {
  buildFeatures.androidResources = false
}"""

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib1
                   dependency    name                       source    build file
                ✔                disableAndroidResources              /lib1/build.gradle.kts: (6, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `unused resource generation without buildFeatures block should be fixed`() {

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
          mindSdk(21)
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
        mindSdk(21)
        buildFeatures.androidResources = false
      }"""

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
              :lib1
                     dependency    name                       source    build file
                  ✔                disableAndroidResources              /lib1/build.gradle.kts:

          ModuleCheck found 1 issue
          """
  }

  @Test
  fun `unused resource generation without android block should add android block under existing plugins block`() {

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
        buildFeatures {
          androidResources = false
        }
      }"""

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
              :lib1
                     dependency    name                       source    build file
                  ✔                disableAndroidResources              /lib1/build.gradle.kts:

          ModuleCheck found 1 issue
          """
  }

  @Test
  fun `unused resource generation without android or plugins block should add android block above dependencies block`() {

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = androidProject(":lib1", "com.modulecheck.lib1") {
      buildFile.writeKotlin(
        """
        apply(plugin = "com.android.library")
        apply(plugin = "org.jetbrains.kotlin-android")

        dependencies {
        }
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
              :lib1
                     dependency    name                       source    build file
                  ✔                disableAndroidResources              /lib1/build.gradle.kts:

          ModuleCheck found 1 issue
          """
  }

  @Test
  fun `unused resource generation when fully qualified should be fixed`() {

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
        android.buildFeatures.androidResources = true
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """
      plugins {
        id("com.android.library")
        kotlin("android")
      }
      android.buildFeatures.androidResources = false
      """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib1
                   dependency    name                       source    build file
                ✔                disableAndroidResources              /lib1/build.gradle.kts: (5, 1):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `unused resource generation when qualified and then scoped should be fixed`() {

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
          androidResources = true
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
      android.buildFeatures {
        androidResources = false
      }
      """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
          :lib1
                 dependency    name                       source    build file
              ✔                disableAndroidResources              /lib1/build.gradle.kts: (6, 3):

      ModuleCheck found 1 issue
      """
  }

  @Test
  fun `unused resource generation when fully scoped should be fixed`() {

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
            androidResources = true
          }
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
          buildFeatures {
            androidResources = false
          }
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib1
                   dependency    name                       source    build file
                ✔                disableAndroidResources              /lib1/build.gradle.kts: (7, 5):

        ModuleCheck found 1 issue
    """
  }

  @Disabled("https://github.com/RBusarow/ModuleCheck/issues/255")
  @Test
  fun `unused resource generation with autocorrect and no explicit buildFeatures property should be fixed`() {

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
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe false

    lib1.buildFile.readText() shouldBe """
      plugins {
        id("com.android.library")
        kotlin("android")
      }
      """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Disabled("https://github.com/RBusarow/ModuleCheck/issues/255")
  @Test
  fun `unused resource generation with autocorrect and no android block should be fixed`() {

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
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe false

    lib1.buildFile.readText() shouldBe """plugins {
  id("com.android.library")
  kotlin("android")
}"""

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }
}
