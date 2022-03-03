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

import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.runtime.test.ProjectFindingReport.overshot
import modulecheck.runtime.test.ProjectFindingReport.unusedDependency
import modulecheck.runtime.test.RunnerTest
import org.junit.jupiter.api.Test

class OverShotDependenciesTest : RunnerTest() {

  @Test
  fun `overshot as api but used in test without auto-correct should fail`() {

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

    run(autoCorrect = false).isSuccess shouldBe false

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        overshot(
          fixed = false,
          configuration = "testImplementation",
          dependency = ":lib1",
          position = null
        ),
        unusedDependency(
          fixed = false,
          configuration = "api",
          dependency = ":lib1",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `overshot as implementation but used in debug without auto-correct should fail`() {

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

    run(autoCorrect = false).isSuccess shouldBe false

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        overshot(
          fixed = false,
          configuration = "debugImplementation",
          dependency = ":lib1",
          position = null
        ),
        unusedDependency(
          fixed = false,
          configuration = "implementation",
          dependency = ":lib1",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `overshot as api but used in test with auto-correct should be fixed`() {

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

    run().isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          // api(project(path = ":lib1"))  // ModuleCheck finding [unusedDependency]
          testImplementation(project(path = ":lib1"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        overshot(
          fixed = true,
          configuration = "testImplementation",
          dependency = ":lib1",
          position = null
        ),
        unusedDependency(
          fixed = true,
          configuration = "api",
          dependency = ":lib1",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `overshot in android project as implementation but used in debug with auto-correct should be fixed`() {

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """.trimIndent()
      )
    }

    val lib2 = androidProject(":lib2", "com.modulecheck.lib2") {
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

    run().isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [unusedDependency]
          debugImplementation(project(path = ":lib1"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        overshot(
          fixed = true,
          configuration = "debugImplementation",
          dependency = ":lib1",
          position = null
        ),
        unusedDependency(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `overshot in non-android as implementation but used in debug with auto-correct should be fixed with quotes`() {

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

    run().isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [unusedDependency]
          "debugImplementation"(project(path = ":lib1"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        overshot(
          fixed = true,
          configuration = "debugImplementation",
          dependency = ":lib1",
          position = null
        ),
        unusedDependency(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `overshot as api but used in test with another testFixture with auto-correct should be fixed`() {

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

    run().isSuccess shouldBe true

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

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        overshot(
          fixed = true,
          configuration = "testImplementation",
          dependency = ":lib1",
          position = null
        ),
        unusedDependency(
          fixed = true,
          configuration = "api",
          dependency = ":lib1",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `overshot as api with config block and comment with auto-correct should be fixed`() {

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

    run().isSuccess shouldBe true

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

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        overshot(
          fixed = true,
          configuration = "testImplementation",
          dependency = ":lib1",
          position = null
        ),
        unusedDependency(
          fixed = true,
          configuration = "api",
          dependency = ":lib1",
          position = "7, 3"
        )
      )
    )
  }

  @Test
  fun `overshot testFixture as api but used in test with another testFixture with auto-correct should be fixed`() {

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
      addDependency(ConfigurationName("testFixturesApi"), lib1, asTestFixture = true)

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

    run().isSuccess shouldBe true

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

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        overshot(
          fixed = true,
          configuration = "testImplementation",
          dependency = ":lib1",
          position = null
        ),
        unusedDependency(
          fixed = true,
          configuration = "api",
          dependency = ":lib1",
          position = "6, 3"
        )
      )
    )
  }
}
