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
import modulecheck.parsing.ConfigurationName
import modulecheck.parsing.SourceSetName
import modulecheck.parsing.asConfigurationName
import org.junit.jupiter.api.Test

class InheritedDependenciesTest : ProjectTest() {

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
  fun `inherited from api dependency without auto-correct should fail`() {

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        open class Lib1Class
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

        import com.modulecheck.lib1.Lib1Class

        open class Lib2Class : Lib1Class()
        """.trimIndent()
      )
    }

    val lib3 = project(":lib3") {
      addDependency(ConfigurationName.api, lib2)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib2"))
        }
        """.trimIndent()
      )
      addSource(
        "com/modulecheck/lib3/Lib3Class.kt",
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        val clazz = Lib1Class()
        val clazz2 = Lib2Class()
        """.trimIndent()
      )
    }

    runner.run(allProjects()).isSuccess shouldBe false

    lib3.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib2"))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib3
                   dependency    name                   source    build file
                X  :lib1         inheritedDependency    :lib2     /lib3/build.gradle.kts: (6, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `inherited from api dependency with auto-correct should be fixed`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        open class Lib1Class
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

        import com.modulecheck.lib1.Lib1Class

        open class Lib2Class : Lib1Class()
        """.trimIndent()
      )
    }

    val lib3 = project(":lib3") {
      addDependency(ConfigurationName.api, lib2)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib2"))
        }
        """.trimIndent()
      )
      addSource(
        "com/modulecheck/lib3/Lib3Class.kt",
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        val clazz = Lib1Class()
        val clazz2 = Lib2Class()
        """.trimIndent()
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib3.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          api(project(path = ":lib2"))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib3
                   dependency    name                   source    build file
                ✔  :lib1         inheritedDependency    :lib2     /lib3/build.gradle.kts: (6, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `inherited from implementation dependency with auto-correct should be fixed`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        open class Lib1Class
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

        import com.modulecheck.lib1.Lib1Class

        open class Lib2Class : Lib1Class()
        """.trimIndent()
      )
    }

    val lib3 = project(":lib3") {
      addDependency(ConfigurationName.implementation, lib2)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib2"))
        }
        """.trimIndent()
      )
      addSource(
        "com/modulecheck/lib3/Lib3Class.kt",
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        private val clazz = Lib1Class()
        private val clazz2 = Lib2Class()
        """.trimIndent()
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib3.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
          implementation(project(path = ":lib2"))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib3
                   dependency    name                   source    build file
                ✔  :lib1         inheritedDependency    :lib2     /lib3/build.gradle.kts: (6, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `inherited from implementation dependency and part of API with auto-correct should be fixed as api config`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        open class Lib1Class
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

        import com.modulecheck.lib1.Lib1Class

        open class Lib2Class : Lib1Class()
        """.trimIndent()
      )
    }

    val lib3 = project(":lib3") {
      addDependency(ConfigurationName.implementation, lib2)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib2"))
        }
        """.trimIndent()
      )
      addSource(
        "com/modulecheck/lib3/Lib3Class.kt",
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        val clazz = Lib1Class()
        private val clazz2 = Lib2Class()
        """.trimIndent()
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib3.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          implementation(project(path = ":lib2"))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib3
                   dependency    name                   source    build file
                ✔  :lib1         inheritedDependency    :lib2     /lib3/build.gradle.kts: (6, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `deeply inherited from testImplementation dependency with auto-correct should be fixed as testImplementation`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        open class Lib1Class
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

        import com.modulecheck.lib1.Lib1Class

        open class Lib2Class : Lib1Class()
        """.trimIndent()
      )
    }

    val lib3 = project(":lib3") {
      addDependency(ConfigurationName.api, lib2)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib2"))
        }
        """.trimIndent()
      )
      addSource(
        "com/modulecheck/lib3/Lib3Class.kt",
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class

        open class Lib3Class : Lib1Class()
        """.trimIndent()
      )
    }

    val lib4 = project(":lib4") {
      addDependency(ConfigurationName.testImplementation, lib3)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(project(path = ":lib3"))
        }
        """.trimIndent()
      )
      addSource(
        "com/modulecheck/lib4/Lib4Class.kt",
        """
        package com.modulecheck.lib4

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class
        import com.modulecheck.lib3.Lib3Class

        val clazz = Lib1Class()
        private val clazz2 = Lib2Class()
        private val clazz3 = Lib3Class()
        """.trimIndent(),
        SourceSetName.TEST
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib4.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(project(path = ":lib1"))
          testImplementation(project(path = ":lib2"))
          testImplementation(project(path = ":lib3"))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib3
                   dependency    name                   source    build file
                ✔  :lib1         inheritedDependency    :lib2     /lib3/build.gradle.kts: (6, 3):
                ✔  :lib2         unusedDependency                 /lib3/build.gradle.kts: (6, 3):

            :lib4
                   dependency    name                   source    build file
                ✔  :lib1         inheritedDependency    :lib3     /lib4/build.gradle.kts: (6, 3):
                ✔  :lib2         inheritedDependency    :lib3     /lib4/build.gradle.kts: (6, 3):

        ModuleCheck found 4 issues
        """
  }

  @Test
  fun `inherited testFixtures from testFixtures with auto-correct should be fixed as testFixtures via testImplementation`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """.trimIndent(),
        SourceSetName.TEST_FIXTURES
      )
    }

    val lib2 = project(":lib2") {
      addDependency("testFixturesApi".asConfigurationName(), lib1, asTestFixture = true)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testFixturesApi(testFixtures(project(path = ":lib1")))
        }
        """.trimIndent()
      )
      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        open class Lib2Class : Lib1Class()
        """.trimIndent(),
        SourceSetName.TEST_FIXTURES
      )
    }

    val lib3 = project(":lib3") {
      addDependency(ConfigurationName.testImplementation, lib2, asTestFixture = true)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(testFixtures(project(path = ":lib2")))
        }
        """.trimIndent()
      )
      addSource(
        "com/modulecheck/lib3/Lib3Class.kt",
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        val clazz = Lib1Class()
        private val clazz2 = Lib2Class()
        """.trimIndent(),
        SourceSetName.TEST
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib3.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(testFixtures(project(path = ":lib1")))
          testImplementation(testFixtures(project(path = ":lib2")))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib3
                   dependency    name                   source    build file
                ✔  :lib1         inheritedDependency    :lib2     /lib3/build.gradle.kts: (6, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `inherited testFixtures from api with auto-correct should be fixed as testFixtures via testImplementation`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """.trimIndent(),
        SourceSetName.TEST_FIXTURES
      )
    }

    val lib2 = project(":lib2") {
      addDependency("testFixturesApi".asConfigurationName(), lib1, asTestFixture = true)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(testFixtures(project(path = ":lib1")))
        }
        """.trimIndent()
      )
      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        open class Lib2Class : Lib1Class()
        """.trimIndent(),
        SourceSetName.TEST_FIXTURES
      )
    }

    val lib3 = project(":lib3") {
      addDependency(ConfigurationName.testImplementation, lib2, asTestFixture = true)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(testFixtures(project(path = ":lib2")))
        }
        """.trimIndent()
      )
      addSource(
        "com/modulecheck/lib3/Lib3Class.kt",
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        val clazz = Lib1Class()
        private val clazz2 = Lib2Class()
        """.trimIndent(),
        SourceSetName.TEST
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib3.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(testFixtures(project(path = ":lib1")))
          testImplementation(testFixtures(project(path = ":lib2")))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib3
                   dependency    name                   source    build file
                ✔  :lib1         inheritedDependency    :lib2     /lib3/build.gradle.kts: (6, 3):

        ModuleCheck found 1 issue
        """
  }
}
