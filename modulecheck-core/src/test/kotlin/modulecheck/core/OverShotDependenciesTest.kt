/*
 * Copyright (C) 2021-2023 Rick Busarow
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

import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.sourceset.SourceSetName
import modulecheck.runtime.test.ProjectFindingReport.overshot
import modulecheck.runtime.test.ProjectFindingReport.unsortedDependencies
import modulecheck.runtime.test.ProjectFindingReport.unusedDependency
import modulecheck.runtime.test.RunnerTest
import org.junit.jupiter.api.Test

class OverShotDependenciesTest : RunnerTest() {

  @Test
  fun `overshot as api but used in test without auto-correct should fail`() = test {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """.trimIndent()
      )
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.api, lib1)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        open class Lib2Class : Lib1Class()
        """.trimIndent(),
        SourceSetName.TEST
      )
    }

    run(autoCorrect = false).isSuccess shouldBe false

    lib2.buildFile shouldHaveText """
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
          position = "6, 3"
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
  fun `overshot as implementation but used in debug without auto-correct should fail`() = test {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """.trimIndent()
      )
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        open class Lib2Class : Lib1Class()
        """.trimIndent(),
        SourceSetName.DEBUG
      )
    }

    run(autoCorrect = false).isSuccess shouldBe false

    lib2.buildFile shouldHaveText """
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
          configuration = "debugApi",
          dependency = ":lib1",
          position = "6, 3"
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
  fun `overshot as api but used in test with auto-correct should be fixed`() = test {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """.trimIndent()
      )
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.api, lib1)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        open class Lib2Class : Lib1Class()
        """.trimIndent(),
        SourceSetName.TEST
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(project(path = ":lib1"))
          // api(project(path = ":lib1"))  // ModuleCheck finding [unused-dependency]
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        overshot(
          fixed = true,
          configuration = "testImplementation",
          dependency = ":lib1",
          position = "6, 3"
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
  fun `overshot as implementation should be debugApi`() = test {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """.trimIndent()
      )
    }

    val lib2 = androidLibrary(":lib2", "com.modulecheck.lib2") {
      addDependency(ConfigurationName.implementation, lib1)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        open class Lib2Class : Lib1Class()
        """.trimIndent(),
        SourceSetName.DEBUG
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          debugApi(project(path = ":lib1"))
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [unused-dependency]
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        overshot(
          fixed = true,
          configuration = "debugApi",
          dependency = ":lib1",
          position = "6, 3"
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
  fun `overshot as implementation should be debugImplementation`() = test {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """.trimIndent()
      )
    }

    val lib2 = androidLibrary(":lib2", "com.modulecheck.lib2") {
      addDependency(ConfigurationName.implementation, lib1)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        private val lib1Class = Lib1Class()
        """.trimIndent(),
        SourceSetName.DEBUG
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          debugImplementation(project(path = ":lib1"))
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [unused-dependency]
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        overshot(
          fixed = true,
          configuration = "debugImplementation",
          dependency = ":lib1",
          position = "6, 3"
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
  fun `overshot in non-android as implementation should be debugApi with quotes`() = test {
    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """.trimIndent()
      )
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        open class Lib2Class : Lib1Class()
        """.trimIndent(),
        SourceSetName.DEBUG
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          "debugApi"(project(path = ":lib1"))
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [unused-dependency]
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        overshot(
          fixed = true,
          configuration = "debugApi",
          dependency = ":lib1",
          position = "6, 3"
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
  fun `overshot as api but used in test with another testFixture with auto-correct should be fixed`() =
    test {

      val lib1 = kotlinProject(":lib1") {
        addKotlinSource(
          """
        package com.modulecheck.lib1

        open class Lib1Class
          """.trimIndent()
        )
      }

      val lib2 = kotlinProject(":lib2") {
        addDependency(ConfigurationName("testFixturesApi"), lib1)

        addKotlinSource(
          """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        open class Lib2Class : Lib1Class()
          """.trimIndent(),
          SourceSetName.TEST_FIXTURES
        )
      }

      val lib3 = kotlinProject(":lib3") {
        addDependency(ConfigurationName.api, lib1)
        addDependency(ConfigurationName.testImplementation, lib2, asTestFixture = true)

        buildFile {
          """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          testImplementation(testFixtures(project(path = ":lib2")))
        }
        """
        }
        addKotlinSource(
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

      lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(project(path = ":lib1"))
          // api(project(path = ":lib1"))  // ModuleCheck finding [unused-dependency]
          testImplementation(testFixtures(project(path = ":lib2")))
        }
    """

      logger.parsedReport() shouldBe listOf(
        ":lib3" to listOf(
          overshot(
            fixed = true,
            configuration = "testImplementation",
            dependency = ":lib1",
            position = "6, 3"
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
  fun `unused in main but used in both test and androidTest should be added to both`() = test {

    settings.checks.sortDependencies = true

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        interface Lib1Interface
        """.trimIndent()
      )
    }

    val lib2 = androidLibrary(":lib2", "com.modulecheck.lib2") {

      addDependency(ConfigurationName.api, lib1)

      buildFile {
        """
        plugins {
          id("com.android.library")
          kotlin("android")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib2.debug

        import com.modulecheck.lib1.Lib1Interface

        class Lib2ClassAndroidTest : Lib1Interface
        """.trimIndent(),
        SourceSetName.ANDROID_TEST
      )
      addKotlinSource(
        """
        package com.modulecheck.lib2.test

        import com.modulecheck.lib1.Lib1Interface

        class Lib2ClassTest : Lib1Interface
        """.trimIndent(),
        SourceSetName.TEST
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
      plugins {
        id("com.android.library")
        kotlin("android")
      }

      dependencies {
        androidTestImplementation(project(path = ":lib1"))

        testImplementation(project(path = ":lib1"))
      }
      """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        unsortedDependencies(fixed = true),
        overshot(
          fixed = true,
          configuration = "androidTestImplementation",
          dependency = ":lib1",
          position = "7, 3"
        ),
        overshot(
          fixed = true,
          configuration = "testImplementation",
          dependency = ":lib1",
          position = "7, 3"
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
  fun `overshot as api with config block and comment with auto-correct should be fixed`() = test {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """.trimIndent()
      )
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.api, lib1)

      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        open class Lib2Class : Lib1Class()
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addDependency(ConfigurationName.api, lib1)
      addDependency(ConfigurationName.testImplementation, lib2)

      buildFile {
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
        """
      }
      addKotlinSource(
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

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          // a comment
          testImplementation(project(path = ":lib1")) {
            because("this is a test")
          }
          // a comment
          // api(project(path = ":lib1")) {
            // because("this is a test")
          // }  // ModuleCheck finding [unused-dependency]
          testImplementation(project(path = ":lib2"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        overshot(
          fixed = true,
          configuration = "testImplementation",
          dependency = ":lib1",
          position = "7, 3"
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
  fun `overshot testFixture as api but used in test with another testFixture with auto-correct should be fixed`() =
    test {

      val lib1 = kotlinProject(":lib1") {
        addKotlinSource(
          """
        package com.modulecheck.lib1

        open class Lib1Class
          """.trimIndent(),
          SourceSetName.TEST_FIXTURES
        )
      }

      val lib2 = kotlinProject(":lib2") {
        addDependency(ConfigurationName("testFixturesApi"), lib1, asTestFixture = true)

        buildFile {
          """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testFixturesApi(testFixtures(project(path = ":lib1")))
        }
        """
        }

        addKotlinSource(
          """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        open class Lib2Class : Lib1Class()
          """.trimIndent(),
          SourceSetName.TEST_FIXTURES
        )
      }

      val lib3 = kotlinProject(":lib3") {
        addDependency(ConfigurationName.api, lib1, asTestFixture = true)
        addDependency(ConfigurationName.testImplementation, lib2, asTestFixture = true)

        buildFile {
          """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(testFixtures(project(path = ":lib1")))
          testImplementation(testFixtures(project(path = ":lib2")))
        }
        """
        }
        addKotlinSource(
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

      lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testFixturesApi(testFixtures(project(path = ":lib1")))
        }
    """

      lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(testFixtures(project(path = ":lib1")))
          // api(testFixtures(project(path = ":lib1")))  // ModuleCheck finding [unused-dependency]
          testImplementation(testFixtures(project(path = ":lib2")))
        }
    """

      logger.parsedReport() shouldBe listOf(
        ":lib3" to listOf(
          overshot(
            fixed = true,
            configuration = "testImplementation",
            dependency = ":lib1",
            position = "6, 3"
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
  fun `overshot as testImplementation from invisible dependency with a visible unrelated api project dependency`() =
    test {

      val lib1 = kotlinProject(":lib1") {
        addKotlinSource(
          """
        package com.modulecheck.lib1

        open class Lib1Class
          """.trimIndent()
        )
      }

      val lib2 = kotlinProject(":lib2") {
        // lib1 is added as a dependency, but it's not in the build file.
        // This is intentional, because it mimics the behavior of a convention plugin
        // which adds a dependency without any visible declaration in the build file
        addDependency(ConfigurationName.api, lib1, addToBuildFile = false)

        buildFile {
          """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib4"))
        }
        """
        }
        addKotlinSource(
          """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        val clazz = Lib1Class()
          """.trimIndent(),
          SourceSetName.TEST
        )
      }

      kotlinProject(":lib4")

      run().isSuccess shouldBe false

      lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(project(path = ":lib1"))
          api(project(path = ":lib4"))
        }
    """

      logger.parsedReport() shouldBe listOf(
        ":lib2" to listOf(
          overshot(
            fixed = true,
            configuration = "testImplementation",
            dependency = ":lib1",
            position = "6, 3"
          ),
          unusedDependency(
            fixed = false,
            configuration = "api",
            dependency = ":lib1",
            position = null
          )
        )
      )
    }

  // https://github.com/RBusarow/ModuleCheck/issues/520
  @Test
  fun `overshot as debugApi from invisible dependency with a visible unrelated debugApi project dependency`() =
    test {

      val lib1 = kotlinProject(":lib1") {
        addKotlinSource(
          """
        package com.modulecheck.lib1

        open class Lib1Class
          """.trimIndent()
        )
      }

      val lib2 = androidLibrary(":lib2", "com.modulecheck.lib2") {
        // lib1 is added as a dependency, but it's not in the build file.
        // This is intentional, because it mimics the behavior of a convention plugin
        // which adds a dependency without any visible declaration in the build file
        addDependency(ConfigurationName.api, lib1, addToBuildFile = false)

        buildFile {
          """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          debugApi(project(path = ":lib4"))
        }
        """
        }
        addKotlinSource(
          """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        val clazz = Lib1Class()
          """.trimIndent(),
          SourceSetName.DEBUG
        )
      }

      kotlinProject(":lib4")

      run().isSuccess shouldBe false

      lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          debugApi(project(path = ":lib1"))
          debugApi(project(path = ":lib4"))
        }
    """

      logger.parsedReport() shouldBe listOf(
        ":lib2" to listOf(
          overshot(
            fixed = true,
            configuration = "debugApi",
            dependency = ":lib1",
            position = "6, 3"
          ),
          unusedDependency(
            fixed = false,
            configuration = "api",
            dependency = ":lib1",
            position = null
          )
        )
      )

      logger.clear()

      // this second run should not have an overshot finding, and shouldn't modify the build file
      run().isSuccess shouldBe false

      lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          debugApi(project(path = ":lib1"))
          debugApi(project(path = ":lib4"))
        }
    """

      logger.parsedReport() shouldBe listOf(
        ":lib2" to listOf(
          unusedDependency(
            fixed = false,
            configuration = "api",
            dependency = ":lib1",
            position = null
          )
        )
      )
    }

  @Test
  fun `overshot as testImplementation from invisible dependency with a visible unrelated implementation project dependency`() =
    test {

      val lib1 = kotlinProject(":lib1") {
        addKotlinSource(
          """
        package com.modulecheck.lib1

        open class Lib1Class
          """.trimIndent()
        )
      }

      val lib2 = kotlinProject(":lib2") {
        // lib1 is added as a dependency, but it's not in the build file.
        // This is intentional, because it mimics the behavior of a convention plugin
        // which adds a dependency without any visible declaration in the build file
        addDependency(ConfigurationName.api, lib1, addToBuildFile = false)

        buildFile {
          """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib4"))
        }
        """
        }
        addKotlinSource(
          """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        val clazz = Lib1Class()
          """.trimIndent(),
          SourceSetName.TEST
        )
      }

      kotlinProject(":lib4")

      run().isSuccess shouldBe false

      lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(project(path = ":lib1"))
          implementation(project(path = ":lib4"))
        }
    """

      logger.parsedReport() shouldBe listOf(
        ":lib2" to listOf(
          overshot(
            fixed = true,
            configuration = "testImplementation",
            dependency = ":lib1",
            position = "6, 3"
          ),
          unusedDependency(
            fixed = false,
            configuration = "api",
            dependency = ":lib1",
            position = null
          )
        )
      )
    }

  @Test
  fun `overshot as testImplementation from invisible dependency with an empty multi-line dependencies block`() =
    test {

      val lib1 = kotlinProject(":lib1") {
        addKotlinSource(
          """
        package com.modulecheck.lib1

        open class Lib1Class
          """.trimIndent()
        )
      }

      val lib2 = kotlinProject(":lib2") {
        // lib1 is added as a dependency, but it's not in the build file.
        // This is intentional, because it mimics the behavior of a convention plugin
        // which adds a dependency without any visible declaration in the build file
        addDependency(ConfigurationName.api, lib1, addToBuildFile = false)

        buildFile {
          """
        plugins {
          kotlin("jvm")
        }

        dependencies {
        }
        """
        }
        addKotlinSource(
          """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        private val clazz = Lib1Class()
          """.trimIndent(),
          SourceSetName.TEST
        )
      }

      run().isSuccess shouldBe false

      lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(project(":lib1"))
        }
    """

      logger.parsedReport() shouldBe listOf(
        ":lib2" to listOf(
          overshot(
            fixed = true,
            configuration = "testImplementation",
            dependency = ":lib1",
            position = "6, 3"
          ),
          unusedDependency(
            fixed = false,
            configuration = "api",
            dependency = ":lib1",
            position = null
          )
        )
      )
    }

  @Test
  fun `overshot as testImplementation from invisible dependency with an empty single-line dependencies block`() =
    test {

      val lib1 = kotlinProject(":lib1") {
        addKotlinSource(
          """
        package com.modulecheck.lib1

        open class Lib1Class
          """.trimIndent()
        )
      }

      val lib2 = kotlinProject(":lib2") {
        // lib1 is added as a dependency, but it's not in the build file.
        // This is intentional, because it mimics the behavior of a convention plugin
        // which adds a dependency without any visible declaration in the build file
        addDependency(ConfigurationName.api, lib1, addToBuildFile = false)

        buildFile {
          """
        plugins {
          kotlin("jvm")
        }

        dependencies { }
        """
        }
        addKotlinSource(
          """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        private val clazz = Lib1Class()
          """.trimIndent(),
          SourceSetName.TEST
        )
      }

      run().isSuccess shouldBe false

      lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(project(":lib1"))
        }
    """

      logger.parsedReport() shouldBe listOf(
        ":lib2" to listOf(
          overshot(
            fixed = true,
            configuration = "testImplementation",
            dependency = ":lib1",
            position = "6, 3"
          ),
          unusedDependency(
            fixed = false,
            configuration = "api",
            dependency = ":lib1",
            position = null
          )
        )
      )
    }

  @Test
  fun `overshot as testImplementation from invisible dependency with no dependencies block`() =
    test {

      val lib1 = kotlinProject(":lib1") {
        addKotlinSource(
          """
        package com.modulecheck.lib1

        open class Lib1Class
          """.trimIndent()
        )
      }

      val lib2 = kotlinProject(":lib2") {
        // lib1 is added as a dependency, but it's not in the build file.
        // This is intentional, because it mimics the behavior of a convention plugin
        // which adds a dependency without any visible declaration in the build file
        addDependency(ConfigurationName.api, lib1, addToBuildFile = false)

        buildFile {
          """
        plugins {
          kotlin("jvm")
        }
        """
        }
        addKotlinSource(
          """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        private val clazz = Lib1Class()
          """.trimIndent(),
          SourceSetName.TEST
        )
      }

      run().isSuccess shouldBe false

      lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(project(":lib1"))
        }
    """

      logger.parsedReport() shouldBe listOf(
        ":lib2" to listOf(
          overshot(
            fixed = true,
            configuration = "testImplementation",
            dependency = ":lib1",
            position = "6, 3"
          ),
          unusedDependency(
            fixed = false,
            configuration = "api",
            dependency = ":lib1",
            position = null
          )
        )
      )
    }

  @Test
  fun `overshot as testImplementation from invisible dependency with only external implementation dependency`() =
    test {

      val lib1 = kotlinProject(":lib1") {
        addKotlinSource(
          """
        package com.modulecheck.lib1

        open class Lib1Class
          """.trimIndent()
        )
      }

      val lib2 = kotlinProject(":lib2") {
        // lib1 is added as a dependency, but it's not in the build file.
        // This is intentional, because it mimics the behavior of a convention plugin
        // which adds a dependency without any visible declaration in the build file
        addDependency(ConfigurationName.api, lib1, addToBuildFile = false)

        buildFile {
          """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
        }
        """
        }
        addKotlinSource(
          """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        private val clazz = Lib1Class()
          """.trimIndent(),
          SourceSetName.TEST
        )
      }

      run().isSuccess shouldBe false

      lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(project(":lib1"))

          implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
        }
    """

      logger.parsedReport() shouldBe listOf(
        ":lib2" to listOf(
          overshot(
            fixed = true,
            configuration = "testImplementation",
            dependency = ":lib1",
            position = "6, 3"
          ),
          unusedDependency(
            fixed = false,
            configuration = "api",
            dependency = ":lib1",
            position = null
          )
        )
      )
    }

  @Test
  fun `overshot as implementation from invisible dependency with only external api dependency`() =
    test {

      val lib1 = kotlinProject(":lib1") {
        addKotlinSource(
          """
        package com.modulecheck.lib1

        open class Lib1Class
          """.trimIndent()
        )
      }

      val lib2 = kotlinProject(":lib2") {
        // lib1 is added as a dependency, but it's not in the build file.
        // This is intentional, because it mimics the behavior of a convention plugin
        // which adds a dependency without any visible declaration in the build file
        addDependency(ConfigurationName.api, lib1, addToBuildFile = false)

        buildFile {
          """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
        }
        """
        }
        addKotlinSource(
          """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        private val clazz = Lib1Class()
          """.trimIndent(),
          SourceSetName.TEST
        )
      }

      run().isSuccess shouldBe false

      lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(project(":lib1"))

          api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
        }
    """

      logger.parsedReport() shouldBe listOf(
        ":lib2" to listOf(
          overshot(
            fixed = true,
            configuration = "testImplementation",
            dependency = ":lib1",
            position = "6, 3"
          ),
          unusedDependency(
            fixed = false,
            configuration = "api",
            dependency = ":lib1",
            position = null
          )
        )
      )
    }
}
