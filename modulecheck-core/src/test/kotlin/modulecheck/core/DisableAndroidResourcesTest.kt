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

import modulecheck.api.test.ProjectTest
import modulecheck.api.test.ReportingLogger
import modulecheck.api.test.TestChecksSettings
import modulecheck.api.test.TestSettings
import modulecheck.api.test.writeKotlin
import modulecheck.core.rule.ModuleCheckRuleFactory
import modulecheck.core.rule.MultiRuleFindingFactory
import modulecheck.parsing.ConfigurationName
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class DisableAndroidResourcesTest : ProjectTest() {

  val ruleFactory by resets { ModuleCheckRuleFactory() }

  val baseSettings by resets { TestSettings(checks = TestChecksSettings(disableAndroidResources = true)) }
  val logger by resets { ReportingLogger() }
  val findingFactory by resets {
    MultiRuleFindingFactory(
      baseSettings,
      ruleFactory.create(baseSettings)
    )
  }

  @Test
  fun `resource generation is used in contributing module with no changes`() {

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
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

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
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
      addDependency(ConfigurationName.implementation, lib1)
      androidResourcesEnabled = false

      addSource(
        "com/modulecheck/lib2/Source.kt",
        """
        package com.modulecheck.lib2

        val string = R.string.app_name
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib1.buildFile.readText() shouldBe """plugins {
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

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
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

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
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
  fun `unused resource generation when fully qualified should be fixed`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
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

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
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

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
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

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
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

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
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
