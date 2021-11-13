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
import modulecheck.api.test.TestSettings
import modulecheck.core.rule.ModuleCheckRuleFactory
import modulecheck.core.rule.MultiRuleFindingFactory
import modulecheck.parsing.AnvilGradlePlugin
import modulecheck.parsing.ConfigurationName
import modulecheck.parsing.SourceSetName
import net.swiftzer.semver.SemVer
import org.junit.jupiter.api.Test

class UnusedDependenciesTest : ProjectTest() {

  val ruleFactory by resets { ModuleCheckRuleFactory() }

  val baseSettings by resets { TestSettings() }
  val logger by resets { ReportingLogger() }
  val findingFactory by resets {
    MultiRuleFindingFactory(
      baseSettings,
      ruleFactory.create(baseSettings)
    )
  }

  @Test
  fun `unused without auto-correct should fail`() {

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {}

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
        }
        """.trimIndent()
      )
    }

    runner.run(allProjects()).isSuccess shouldBe false

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name                source    build file
                X  :lib1         unusedDependency              /lib2/build.gradle.kts: (6, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `unused with auto-correct should be commented out`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {}

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
        }
        """.trimIndent()
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [unusedDependency]
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name                source    build file
                ✔  :lib1         unusedDependency              /lib2/build.gradle.kts: (6, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `unused with auto-correct and deleteUnused should be deleted`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings.copy(deleteUnused = true),
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {}

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
        }
        """.trimIndent()
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name                source    build file
                ✔  :lib1         unusedDependency              /lib2/build.gradle.kts: (6, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `unused but suppressed with auto-correct and deleteUnused should not be changed`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings.copy(deleteUnused = true),
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {}

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          @Suppress("unusedDependency")
          implementation(project(path = ":lib1"))
        }
        """.trimIndent()
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          @Suppress("unusedDependency")
          implementation(project(path = ":lib1"))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `unused but suppressed at the block level should not be changed`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings.copy(deleteUnused = true),
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {}

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        @Suppress("unusedDependency")
        dependencies {
          implementation(project(path = ":lib1"))
        }
        """.trimIndent()
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        @Suppress("unusedDependency")
        dependencies {
          implementation(project(path = ":lib1"))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `unused with auto-correct with preceding typesafe external dependency should be deleted`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings.copy(deleteUnused = true),
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {}

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(libs.javax.inject)
          implementation(project(path = ":lib1"))
        }
        """.trimIndent()
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(libs.javax.inject)
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name                source    build file
                ✔  :lib1         unusedDependency              /lib2/build.gradle.kts: (7, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `unused with auto-correct with string extension function for config should be deleted`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings.copy(deleteUnused = true),
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {}

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          "implementation"(project(path = ":lib1"))
        }
        """.trimIndent()
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name                source    build file
                ✔  :lib1         unusedDependency              /lib2/build.gradle.kts: (6, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `unused without auto-correct with string extension function for config should fail`() {

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings.copy(deleteUnused = true),
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {}

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          "implementation"(project(path = ":lib1"))
        }
        """.trimIndent()
      )
    }

    runner.run(allProjects()).isSuccess shouldBe false

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          "implementation"(project(path = ":lib1"))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name                source    build file
                X  :lib1         unusedDependency              /lib2/build.gradle.kts: (6, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `unused with auto-correct and deleteUnused following dependency config block should be deleted`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings.copy(deleteUnused = true),
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {}

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(libs.javax.inject) {
          }
          implementation(project(path = ":lib1"))
        }
        """.trimIndent()
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(libs.javax.inject) {
          }
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name                source    build file
                ✔  :lib1         unusedDependency              /lib2/build.gradle.kts: (8, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `unused with auto-correct following dependency config block should be commented out`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings.copy(deleteUnused = false),
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {}

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(libs.javax.inject) {
          }
          implementation(project(path = ":lib1"))
        }
        """.trimIndent()
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(libs.javax.inject) {
          }
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [unusedDependency]
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name                source    build file
                ✔  :lib1         unusedDependency              /lib2/build.gradle.kts: (8, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `dependencies from non-jvm configurations should be ignored`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings.copy(deleteUnused = false),
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {}

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        configurations.create("fakeConfig")

        dependencies {
          fakeConfig(project(path = ":lib1"))
          implementation(project(path = ":lib1"))
        }
        """.trimIndent()
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        configurations.create("fakeConfig")

        dependencies {
          fakeConfig(project(path = ":lib1"))
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [unusedDependency]
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name                source    build file
                ✔  :lib1         unusedDependency              /lib2/build.gradle.kts: (9, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `testImplementation used in test should not be unused`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings.copy(deleteUnused = false),
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """.trimIndent()
      )
    }

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.testImplementation, lib1)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(project(path = ":lib1"))
        }
        """.trimIndent()
      )

      addSource(
        "com/modulecheck/lib2/Lib2Test.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        val lib1Class = Lib1Class()
        """.trimIndent(),
        SourceSetName.TEST
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(project(path = ":lib1"))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `androidTestImplementation used in androidTest should not be unused`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings.copy(deleteUnused = false),
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = androidProject(":lib1", "com.modulecheck.lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """.trimIndent()
      )
    }

    val lib2 = androidProject(":lib2", "com.modulecheck.lib2") {
      addDependency(ConfigurationName.androidTestImplementation, lib1)

      buildFile.writeText(
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        dependencies {
          androidTestImplementation(project(path = ":lib1"))
        }
        """.trimIndent()
      )

      addSource(
        "com/modulecheck/lib2/Lib2Test.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        val lib1Class = Lib1Class()
        """.trimIndent(),
        SourceSetName.ANDROID_TEST
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        dependencies {
          androidTestImplementation(project(path = ":lib1"))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `custom view used in a layout file should not be unused`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings.copy(deleteUnused = false),
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = androidProject(":lib1", "com.modulecheck.lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1View.kt",
        """
        package com.modulecheck.lib1

        class Lib1View
        """.trimIndent()
      )
    }

    val lib2 = androidProject(":lib2", "com.modulecheck.lib2") {
      addDependency(ConfigurationName.api, lib1)

      buildFile.writeText(
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
        """.trimIndent()
      )

      addLayoutFile(
        "fragment_lib2.xml",
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

    lib2.buildFile.readText() shouldBe """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `module contributing a used generated DataBinding object should not be unused`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings.copy(deleteUnused = false),
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = androidProject(":lib1", "com.modulecheck.lib1") {
      viewBindingEnabled = true

      addLayoutFile(
        "fragment_lib1.xml",
        """<?xml version="1.0" encoding="utf-8"?>
          <layout/>
        """
      )
    }

    val lib2 = androidProject(":lib2", "com.modulecheck.lib2") {
      addDependency(ConfigurationName.api, lib1)

      buildFile.writeText(
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
        """.trimIndent()
      )
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

    lib2.buildFile.readText() shouldBe """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `declaration used via a wildcard import should not be unused`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings.copy(deleteUnused = false),
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """.trimIndent()
      )
    }

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.api, lib1)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
        """.trimIndent()
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.*

        val lib1Class = Lib1Class()
        """.trimIndent()
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `testFixtures declaration used in test should not be unused`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings.copy(deleteUnused = false),
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """.trimIndent(),
        SourceSetName.TEST_FIXTURES
      )
    }

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.testImplementation, lib1, asTestFixture = true)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(testFixtures(project(path = ":lib1")))
        }
        """.trimIndent()
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.*

        val lib1Class = Lib1Class()
        """.trimIndent(),
        SourceSetName.TEST
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(testFixtures(project(path = ":lib1")))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `unused from testFixtures with auto-correct should be fixed`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings.copy(deleteUnused = false),
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """.trimIndent(),
        SourceSetName.TEST_FIXTURES
      )
    }

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.testImplementation, lib1, asTestFixture = true)
      addSourceSet(SourceSetName.TEST)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(testFixtures(project(path = ":lib1")))
        }
        """.trimIndent()
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          // testImplementation(testFixtures(project(path = ":lib1")))  // ModuleCheck finding [unusedDependency]
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name                source    build file
                ✔  :lib1         unusedDependency              /lib2/build.gradle.kts: (6, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `static member declaration used via wildcard import should not be unused`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings.copy(deleteUnused = false),
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class {
          companion object {
            fun build(): Lib1Class = Lib1Class()
          }
        }
        """.trimIndent()
      )
    }

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.api, lib1)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
        """.trimIndent()
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.*

        val lib1Class = Lib1Class.build()
        """.trimIndent()
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `string resource used in module should not be unused`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings.copy(deleteUnused = false),
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = androidProject(":lib1", "com.modulecheck.lib1") {

      addResourceFile(
        "values/strings.xml",
        """
        <resources>
          <string name="app_name" translatable="false">MyApp</string>
        </resources>
        """
      )
    }

    val lib2 = androidProject(":lib2", "com.modulecheck.lib2") {
      addDependency(ConfigurationName.api, lib1)

      buildFile.writeText(
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
        """.trimIndent()
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        val appName = R.string.app_name
        """.trimIndent()
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `string resource used in manifest should not be unused`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings.copy(deleteUnused = false),
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = androidProject(":lib1", "com.modulecheck.lib1") {

      addResourceFile(
        "values/strings.xml",
        """
        <resources>
          <string name="app_name" translatable="false">MyApp</string>
        </resources>
        """
      )
    }

    val lib2 = androidProject(":lib2", "com.modulecheck.lib2") {
      addDependency(ConfigurationName.api, lib1)

      buildFile.writeText(
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
        """.trimIndent()
      )

      addManifest(
        """
          <manifest
            xmlns:android="https://schemas.android.com/apk/res/android"
            package="com.example.app"
            >

            <application
              android:name=".App"
              android:allowBackup="true"
              android:icon="@mipmap/ic_launcher"
              android:label="@string/app_name"
              android:roundIcon="@mipmap/ic_launcher_round"
              android:supportsRtl="true"
              android:theme="@style/AppTheme"
              />
          </manifest>
          """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `declaration used via class reference wtih wildcard import should not be unused`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings.copy(deleteUnused = false),
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        abstract class Lib1Class private constructor()
        """.trimIndent()
      )
    }

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.api, lib1)
      anvilGradlePlugin = AnvilGradlePlugin(SemVer(2, 3, 8), true)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
          id("com.squareup.anvil")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
        """.trimIndent()
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.*
        import com.squareup.anvil.annotations.ContributesTo
        import dagger.Module

        @Module
        @ContributesTo(Lib1Class::class)
        object MyModule
        """.trimIndent()
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
          id("com.squareup.anvil")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }
}
