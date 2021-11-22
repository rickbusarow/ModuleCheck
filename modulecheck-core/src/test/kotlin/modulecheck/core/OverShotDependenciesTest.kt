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

import modulecheck.api.test.ReportingLogger
import modulecheck.api.test.TestSettings
import modulecheck.core.rule.ModuleCheckRuleFactory
import modulecheck.core.rule.MultiRuleFindingFactory
import modulecheck.project.ConfigurationName
import modulecheck.project.SourceSetName
import modulecheck.project.test.ProjectTest
import org.junit.jupiter.api.Test

class OverShotDependenciesTest : ProjectTest() {

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
  fun `overshot as api but used in test without auto-correct should fail`() {

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
        """.trimIndent(),
        SourceSetName.TEST
      )
    }

    runner.run(allProjects()).isSuccess shouldBe false

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
      .clean() shouldBe """
            :lib2
                   dependency    name                source    build file
                X  :lib1         overshot                      /lib2/build.gradle.kts:
                X  :lib1         unusedDependency              /lib2/build.gradle.kts: (6, 3):

        ModuleCheck found 2 issues
        """
  }

  @Test
  fun `overshot as implementation but used in debug without auto-correct should fail`() {

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
      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        open class Lib2Class : Lib1Class()
        """.trimIndent(),
        SourceSetName.DEBUG
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
                X  :lib1         overshot                      /lib2/build.gradle.kts:
                X  :lib1         unusedDependency              /lib2/build.gradle.kts: (6, 3):

        ModuleCheck found 2 issues
        """
  }

  @Test
  fun `overshot as api but used in test with auto-correct should be fixed`() {

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
          // api(project(path = ":lib1"))  // ModuleCheck finding [unusedDependency]
          testImplementation(project(path = ":lib1"))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name                source    build file
                ✔  :lib1         overshot                      /lib2/build.gradle.kts:
                ✔  :lib1         unusedDependency              /lib2/build.gradle.kts: (6, 3):

        ModuleCheck found 2 issues
        """
  }

  @Test
  fun `overshot as implementation but used in debug with auto-correct should be fixed`() {

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
      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        open class Lib2Class : Lib1Class()
        """.trimIndent(),
        SourceSetName.DEBUG
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [unusedDependency]
          debugImplementation(project(path = ":lib1"))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name                source    build file
                ✔  :lib1         overshot                      /lib2/build.gradle.kts:
                ✔  :lib1         unusedDependency              /lib2/build.gradle.kts: (6, 3):

        ModuleCheck found 2 issues
        """
  }

  @Test
  fun `overshot as api but used in test with another testFixture with auto-correct should be fixed`() {

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
      addDependency(ConfigurationName("testFixturesApi"), lib1)

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
      addDependency(ConfigurationName.api, lib1)
      addDependency(ConfigurationName.testImplementation, lib2, asTestFixture = true)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
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

        val lib1Class = Lib1Class()
        val lib2Class = Lib2Class()
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
          // api(project(path = ":lib1"))  // ModuleCheck finding [unusedDependency]
          testImplementation(testFixtures(project(path = ":lib2")))
          testImplementation(project(path = ":lib1"))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib3
                   dependency    name                source    build file
                ✔  :lib1         overshot                      /lib3/build.gradle.kts:
                ✔  :lib1         unusedDependency              /lib3/build.gradle.kts: (6, 3):

        ModuleCheck found 2 issues
        """
  }

  @Test
  fun `overshot as api with config block and comment with auto-correct should be fixed`() {

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
      addDependency(ConfigurationName.api, lib1)
      addDependency(ConfigurationName.testImplementation, lib2)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          // a comment
          api(project(path = ":lib1")) {
            because("this is a test")
          }
          testImplementation(project(path = ":lib2"))
        }
        """.trimIndent()
      )
      addSource(
        "com/modulecheck/lib3/Lib3Class.kt",
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        val lib1Class = Lib1Class()
        val lib2Class = Lib2Class()
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
          // // a comment
          // api(project(path = ":lib1")) {
            // because("this is a test")
          // }  // ModuleCheck finding [unusedDependency]
          testImplementation(project(path = ":lib2"))
          // a comment
          testImplementation(project(path = ":lib1")) {
            because("this is a test")
          }
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib3
                   dependency    name                source    build file
                ✔  :lib1         overshot                      /lib3/build.gradle.kts:
                ✔  :lib1         unusedDependency              /lib3/build.gradle.kts: (7, 3):

        ModuleCheck found 2 issues
        """
  }

  @Test
  fun `overshot testFixture as api but used in test with another testFixture with auto-correct should be fixed`() {

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
      addDependency(ConfigurationName("testFixturesApi"), lib1)

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
      addDependency(ConfigurationName.api, lib1, asTestFixture = true)
      addDependency(ConfigurationName.testImplementation, lib2, asTestFixture = true)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(testFixtures(project(path = ":lib1")))
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

        val lib1Class = Lib1Class()
        val lib2Class = Lib2Class()
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
          // api(testFixtures(project(path = ":lib1")))  // ModuleCheck finding [unusedDependency]
          testImplementation(testFixtures(project(path = ":lib2")))
          testImplementation(testFixtures(project(path = ":lib1")))
        }
        """

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib3
                   dependency    name                source    build file
                ✔  :lib1         overshot                      /lib3/build.gradle.kts:
                ✔  :lib1         unusedDependency              /lib3/build.gradle.kts: (6, 3):

        ModuleCheck found 2 issues
        """
  }
}
