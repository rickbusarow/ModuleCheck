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

import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.asConfigurationName
import modulecheck.model.sourceset.SourceSetName
import modulecheck.model.sourceset.asSourceSetName
import modulecheck.runtime.test.ProjectFindingReport.inheritedDependency
import modulecheck.runtime.test.ProjectFindingReport.mustBeApi
import modulecheck.runtime.test.ProjectFindingReport.overshot
import modulecheck.runtime.test.ProjectFindingReport.unusedDependency
import modulecheck.runtime.test.RunnerTest
import org.junit.jupiter.api.Test

class InheritedDependenciesTest : RunnerTest() {

  @Test
  fun `inherited from api dependency without auto-correct should fail`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addDependency(ConfigurationName.api, lib2)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib2"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        val clazz = Lib1Class()
        val clazz2 = Lib2Class()
        """.trimIndent()
      )
    }

    run(
      autoCorrect = false
    ).isSuccess shouldBe false

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib2"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = false,
          configuration = "api",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `not inherited when source only declares as implementation config`() {

    // A Kotlin build of this kotlinProject would actually fail since :lib1 isn't in :lib3's classpath,
    // but the test is still useful since it's just assuring that behavior is consistent

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

        private val clazz = Lib1Class()

        open class Lib2Class
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addDependency(ConfigurationName.api, lib2)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib2"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        val clazz = Lib1Class()
        val clazz2 = Lib2Class()
        """.trimIndent()
      )
    }

    run(
      autoCorrect = false,
      strictResolution = false
    ).isSuccess shouldBe true

    logger.parsedReport() shouldBe listOf()

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib2"))
        }
    """
  }

  @Test
  fun `not inherited when target and subject are the same`() {

    val lib1 = kotlinProject(":lib1") {

      addKotlinSource(
        """
        package com.modulecheck.lib1

        import com.modulecheck.lib2.Lib2Class

        abstract class Lib1Class : Lib2Class()
        """.trimIndent()
      )
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.testImplementation, lib1)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(project(path = ":lib1"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib2

        abstract class Lib2Class
        """.trimIndent()
      )
      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        private val clazz : Lib2Class = Lib1Class()
        """.trimIndent(),
        SourceSetName.TEST
      )
    }
    lib1.addDependency(ConfigurationName.api, lib2)

    run(autoCorrect = false).isSuccess shouldBe true

    logger.parsedReport() shouldBe listOf()

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(project(path = ":lib1"))
        }
    """
  }

  @Test
  fun `inherited from api dependency with auto-correct should be fixed`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addDependency(ConfigurationName.api, lib2)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib2"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        val clazz = Lib1Class()
        val clazz2 = Lib2Class()
        """.trimIndent()
      )
    }

    run().isSuccess shouldBe true

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          api(project(path = ":lib2"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "api",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  // https://github.com/RBusarow/ModuleCheck/issues/569
  @Test
  fun `inherited with same-config token with type-safe accessor should also use type-safe accessor`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addDependency(ConfigurationName.api, lib2)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(projects.lib2)
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        val clazz = Lib1Class()
        val clazz2 = Lib2Class()
        """.trimIndent()
      )
    }

    run().isSuccess shouldBe true

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(projects.lib1)
          api(projects.lib2)
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "api",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  // https://github.com/RBusarow/ModuleCheck/issues/569
  @Test
  fun `inherited with different-config token with type-safe accessor should also use type-safe accessor`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addDependency(ConfigurationName.implementation, lib2)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(projects.lib2)
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        val clazz = Lib1Class()
        private val clazz2 = Lib2Class()
        """.trimIndent()
      )
    }

    run().isSuccess shouldBe true

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(projects.lib1)
          implementation(projects.lib2)
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "api",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `inherited with different-config tokens with mixed accessor types but type-safe source should use type-safe`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
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
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class

        open class Lib3Class : Lib1Class()
        """.trimIndent()
      )
    }

    val lib4 = kotlinProject(":lib4") {
      addDependency(ConfigurationName.implementation, lib2)
      addDependency(ConfigurationName.implementation, lib3)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(projects.lib2)
          implementation(project(path = ":lib3"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib4

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class
        import com.modulecheck.lib3.Lib3Class

        val clazz = Lib1Class()
        private val clazz2 = Lib2Class()
        private val clazz3 = Lib3Class()
        """.trimIndent()
      )
    }

    run().isSuccess shouldBe true

    lib4.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(projects.lib1)
          implementation(projects.lib2)
          implementation(project(path = ":lib3"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib4" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "api",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `inherited with different-config tokens with mixed accessor types but path source should use path`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
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
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class

        open class Lib3Class : Lib1Class()
        """.trimIndent()
      )
    }

    val lib4 = kotlinProject(":lib4") {
      addDependency(ConfigurationName.implementation, lib2)
      addDependency(ConfigurationName.implementation, lib3)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib2"))
          implementation(projects.lib3)
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib4

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class
        import com.modulecheck.lib3.Lib3Class

        val clazz = Lib1Class()
        private val clazz2 = Lib2Class()
        private val clazz3 = Lib3Class()
        """.trimIndent()
      )
    }

    run().isSuccess shouldBe true

    lib4.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          implementation(project(path = ":lib2"))
          implementation(projects.lib3)
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib4" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "api",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `inherited as api from invisible dependency with a visible unrelated api project dependency`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib3

        class Lib3Class
        """.trimIndent()
      )
    }

    val lib4 = kotlinProject(":lib4") {
      // lib2 is added as a dependency, but it's not in the build file.
      // This is intentional, because it mimics the behavior of a convention plugin
      // which adds a dependency without any visible declaration in the build file
      addDependency(ConfigurationName.api, lib2, addToBuildFile = false)
      addDependency(ConfigurationName.api, lib3, addToBuildFile = false)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib3"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib4

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class
        import com.modulecheck.lib3.Lib3Class

        val clazz = Lib1Class()
        val clazz2 = Lib2Class()
        val clazz3 = Lib3Class()
        """.trimIndent()
      )
    }

    run().isSuccess shouldBe true

    lib4.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          api(project(path = ":lib3"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib4" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "api",
          dependency = ":lib1",
          source = ":lib2",
          position = null
        )
      )
    )
  }

  @Test
  fun `inherited as api from invisible dependency with a visible unrelated implementation project dependency`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib3

        class Lib3Class
        """.trimIndent()
      )
    }

    val lib4 = kotlinProject(":lib4") {
      // lib2 is added as a dependency, but it's not in the build file.
      // This is intentional, because it mimics the behavior of a convention plugin
      // which adds a dependency without any visible declaration in the build file
      addDependency(ConfigurationName.api, lib2, addToBuildFile = false)
      addDependency(ConfigurationName.implementation, lib3, addToBuildFile = false)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib3"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib4

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class
        import com.modulecheck.lib3.Lib3Class

        val clazz = Lib1Class()
        val clazz2 = Lib2Class()
        private val clazz3 = Lib3Class()
        """.trimIndent()
      )
    }

    run().isSuccess shouldBe true

    lib4.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          implementation(project(path = ":lib3"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib4" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "api",
          dependency = ":lib1",
          source = ":lib2",
          position = null
        )
      )
    )
  }

  @Test
  fun `inherited as implementation from invisible dependency with a visible unrelated api project dependency`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib3

        class Lib3Class
        """.trimIndent()
      )
    }

    val lib4 = kotlinProject(":lib4") {
      // lib2 is added as a dependency, but it's not in the build file.
      // This is intentional, because it mimics the behavior of a convention plugin
      // which adds a dependency without any visible declaration in the build file
      addDependency(ConfigurationName.api, lib2, addToBuildFile = false)
      addDependency(ConfigurationName.api, lib3, addToBuildFile = false)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib3"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib4

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class
        import com.modulecheck.lib3.Lib3Class

        private val clazz = Lib1Class()
        val clazz2 = Lib2Class()
        val clazz3 = Lib3Class()
        """.trimIndent()
      )
    }

    run().isSuccess shouldBe true

    lib4.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
          api(project(path = ":lib3"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib4" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          source = ":lib2",
          position = null
        )
      )
    )
  }

  @Test
  fun `inherited as implementation from invisible dependency with a visible unrelated implementation project dependency`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib3

        class Lib3Class
        """.trimIndent()
      )
    }

    val lib4 = kotlinProject(":lib4") {
      // lib2 is added as a dependency, but it's not in the build file.
      // This is intentional, because it mimics the behavior of a convention plugin
      // which adds a dependency without any visible declaration in the build file
      addDependency(ConfigurationName.api, lib2, addToBuildFile = false)
      addDependency(ConfigurationName.implementation, lib3, addToBuildFile = false)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib3"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib4

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class
        import com.modulecheck.lib3.Lib3Class

        private val clazz = Lib1Class()
        val clazz2 = Lib2Class()
        private val clazz3 = Lib3Class()
        """.trimIndent()
      )
    }

    run().isSuccess shouldBe true

    lib4.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
          implementation(project(path = ":lib3"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib4" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          source = ":lib2",
          position = null
        )
      )
    )
  }

  @Test
  fun `inherited as implementation from invisible dependency with an empty multi-line dependencies block`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      // lib2 is added as a dependency, but it's not in the build file.
      // This is intentional, because it mimics the behavior of a convention plugin
      // which adds a dependency without any visible declaration in the build file
      addDependency(ConfigurationName.api, lib2, addToBuildFile = false)

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
        package com.modulecheck.lib4

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class
        import com.modulecheck.lib3.Lib3Class

        private val clazz = Lib1Class()
        val clazz2 = Lib2Class()
        private val clazz3 = Lib3Class()
        """.trimIndent()
      )
    }

    run().isSuccess shouldBe true

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(":lib1"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          source = ":lib2",
          position = null
        )
      )
    )
  }

  @Test
  fun `inherited as implementation from invisible dependency with an empty single-line dependencies block`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      // lib2 is added as a dependency, but it's not in the build file.
      // This is intentional, because it mimics the behavior of a convention plugin
      // which adds a dependency without any visible declaration in the build file
      addDependency(ConfigurationName.api, lib2, addToBuildFile = false)

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
        package com.modulecheck.lib4

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class
        import com.modulecheck.lib3.Lib3Class

        private val clazz = Lib1Class()
        val clazz2 = Lib2Class()
        private val clazz3 = Lib3Class()
        """.trimIndent()
      )
    }

    run().isSuccess shouldBe true

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(":lib1"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          source = ":lib2",
          position = null
        )
      )
    )
  }

  @Test
  fun `inherited as implementation from invisible dependency with no dependencies block`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      // lib2 is added as a dependency, but it's not in the build file.
      // This is intentional, because it mimics the behavior of a convention plugin
      // which adds a dependency without any visible declaration in the build file
      addDependency(ConfigurationName.api, lib2, addToBuildFile = false)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib4

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class
        import com.modulecheck.lib3.Lib3Class

        private val clazz = Lib1Class()
        val clazz2 = Lib2Class()
        private val clazz3 = Lib3Class()
        """.trimIndent()
      )
    }

    run().isSuccess shouldBe true

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(":lib1"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          source = ":lib2",
          position = null
        )
      )
    )
  }

  @Test
  fun `inherited as implementation from invisible dependency with only external implementation dependency`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      // lib2 is added as a dependency, but it's not in the build file.
      // This is intentional, because it mimics the behavior of a convention plugin
      // which adds a dependency without any visible declaration in the build file
      addDependency(ConfigurationName.api, lib2, addToBuildFile = false)

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
        package com.modulecheck.lib4

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class
        import com.modulecheck.lib3.Lib3Class

        private val clazz = Lib1Class()
        val clazz2 = Lib2Class()
        private val clazz3 = Lib3Class()
        """.trimIndent()
      )
    }

    run().isSuccess shouldBe true

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(":lib1"))

          implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          source = ":lib2",
          position = null
        )
      )
    )
  }

  @Test
  fun `inherited as implementation from invisible dependency with only external api dependency`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      // lib2 is added as a dependency, but it's not in the build file.
      // This is intentional, because it mimics the behavior of a convention plugin
      // which adds a dependency without any visible declaration in the build file
      addDependency(ConfigurationName.api, lib2, addToBuildFile = false)

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
        package com.modulecheck.lib4

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class
        import com.modulecheck.lib3.Lib3Class

        private val clazz = Lib1Class()
        val clazz2 = Lib2Class()
        private val clazz3 = Lib3Class()
        """.trimIndent()
      )
    }

    run().isSuccess shouldBe true

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(":lib1"))

          api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          source = ":lib2",
          position = null
        )
      )
    )
  }

  @Test
  fun `inherited as internalImplementation from internalApi dependency with auto-correct should be fixed`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addDependency("internalApi".asConfigurationName(), lib2)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          "internalApi"(project(path = ":lib2"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        private val clazz = Lib1Class()
        val clazz2 = Lib2Class()
        """.trimIndent(),
        SourceSetName("internal")
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
    """

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          "internalImplementation"(project(path = ":lib1"))
          "internalApi"(project(path = ":lib2"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "internalImplementation",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `inherited from dependency with config block should not duplicate config block`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addDependency(ConfigurationName.api, lib2)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib2")) {
            exclude(group = "androidx.appcompat")
          }
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
        """.trimIndent()
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
    """

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          api(project(path = ":lib2")) {
            exclude(group = "androidx.appcompat")
          }
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "api",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `inherited from dependency with comments should not duplicate the comments`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addDependency(ConfigurationName.api, lib2)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          // preceding comment
          api(project(path = ":lib2")) // trailing comment
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
        """.trimIndent()
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
    """

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          // preceding comment
          api(project(path = ":lib2")) // trailing comment
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "api",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `inherited as in main but also used in debug should only be added to main`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addDependency(ConfigurationName.api, lib2)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib2"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        val clazz = Lib1Class()
        val clazz2 = Lib2Class()
        """.trimIndent()
      )
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class

        val lib1Class = Lib1Class()
        """.trimIndent(),
        sourceSetName = SourceSetName.DEBUG
      )
    }

    run().isSuccess shouldBe true

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          api(project(path = ":lib2"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "api",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `inherited as implementation in debug should be added to debug`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """.trimIndent(),
        SourceSetName.DEBUG
      )
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency("debugApi".asConfigurationName(), lib1)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          debugApi(project(path = ":lib1"))
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

    val lib3 = androidLibrary(":lib3", "com.modulecheck.lib3") {
      addDependency("debugImplementation".asConfigurationName(), lib2)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          debugImplementation(project(path = ":lib2"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        val lib1Class = Lib1Class()
        private val lib2Class = Lib2Class()
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
        }
    """

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          debugApi(project(path = ":lib1"))
          debugImplementation(project(path = ":lib2"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "debugApi",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `inherited as internalImplementation from api dependency with auto-correct should be fixed`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addDependency(ConfigurationName.api, lib2)
      addSourceSet(SourceSetName("internal"), upstreamNames = listOf(SourceSetName.MAIN))

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib2"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib2.Lib2Class

        val clazz2 = Lib2Class()
        """.trimIndent()
      )
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class

        private val clazz = Lib1Class()
        """.trimIndent(),
        SourceSetName("internal")
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
    """

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          "internalImplementation"(project(path = ":lib1"))
          api(project(path = ":lib2"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "internalImplementation",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `inherited into mid-level source set should not also be added to downstream source sets`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addDependency(ConfigurationName.api, lib2)
      addSourceSet(SourceSetName("middle"), upstreamNames = listOf(SourceSetName.MAIN))
      addSourceSet(
        SourceSetName("leafOne"),
        upstreamNames = listOf("middle".asSourceSetName())
      )
      addSourceSet(
        SourceSetName("leafTwo"),
        upstreamNames = listOf("middle".asSourceSetName())
      )

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib2"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib2.Lib2Class

        val clazz2 = Lib2Class()
        """.trimIndent()
      )
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class

        private val clazz = Lib1Class()
        """.trimIndent(),
        SourceSetName("middle")
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
        }
    """

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          "middleImplementation"(project(path = ":lib1"))
          api(project(path = ":lib2"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "middleImplementation",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `non-standard config name should be invoked as function if it's already used that way`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {

      addSourceSet("internal".asSourceSetName(), upstreamNames = listOf(SourceSetName.MAIN))

      addDependency("internalImplementation".asConfigurationName(), lib2)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          internalImplementation(project(path = ":lib2"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        private val clazz = Lib1Class()
        private val clazz2 = Lib2Class()
        """.trimIndent(),
        SourceSetName("internal")
      )
    }

    run().isSuccess shouldBe true

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          internalImplementation(project(path = ":lib1"))
          internalImplementation(project(path = ":lib2"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "internalImplementation",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `inherited via testApi should not cause infinite loop`() {

    val lib1 = kotlinProject(":lib1") {

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib2"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib1

        import com.modulecheck.lib2.Lib2Class

        open class Lib1Class
        private val lib2Class = Lib2Class()
        """.trimIndent()
      )
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.testApi, lib1)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testApi(project(path = ":lib1"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib2

        class Lib2Class
        """.trimIndent()
      )
      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        val lib1Class = Lib1Class()
        """.trimIndent(),
        SourceSetName.TEST
      )
    }

    lib1.addDependency(ConfigurationName.implementation, lib2)

    run().isSuccess shouldBe true

    logger.parsedReport() shouldBe listOf()

    lib1.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib2"))
        }
    """

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testApi(project(path = ":lib1"))
        }
    """
  }

  @Test
  fun `inherited from implementation dependency with auto-correct should be fixed`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addDependency(ConfigurationName.implementation, lib2)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib2"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        private val clazz = Lib1Class()
        private val clazz2 = Lib2Class()
        """.trimIndent()
      )
    }

    run().isSuccess shouldBe true

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
          implementation(project(path = ":lib2"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `inherited implementation from implementation with string extension should be added with string invocation`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addDependency(ConfigurationName.implementation, lib2)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          "implementation"(project(path = ":lib2"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        private val clazz = Lib1Class()
        private val clazz2 = Lib2Class()
        """.trimIndent()
      )
    }

    run().isSuccess shouldBe true

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          "implementation"(project(path = ":lib1"))
          "implementation"(project(path = ":lib2"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `inherited implementation from api with string extension should be added with precompiled function`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addDependency(ConfigurationName.api, lib2)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          "api"(project(path = ":lib2"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        private val clazz = Lib1Class()
        val clazz2 = Lib2Class()
        """.trimIndent()
      )
    }

    run().isSuccess shouldBe true

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
          "api"(project(path = ":lib2"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `inherited novel debugImplementation in android project should use precompiled function`() {

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

        open class Lib2Class
        """.trimIndent()
      )
    }

    val lib3 = androidLibrary(":lib3", "com.modulecheck.lib3") {
      addDependency(ConfigurationName.api, lib2)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib2"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib2.Lib2Class

        val clazz2 = Lib2Class()
        """.trimIndent()
      )
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class

        private val clazz = Lib1Class()
        """.trimIndent(),
        SourceSetName.DEBUG
      )
    }

    run().isSuccess shouldBe true

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          debugImplementation(project(path = ":lib1"))
          api(project(path = ":lib2"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "debugImplementation",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `inherited novel androidTestDebugImplementation in android project should use precompiled function`() {

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

        open class Lib2Class
        """.trimIndent()
      )
    }

    val lib3 = androidLibrary(":lib3", "com.modulecheck.lib3") {
      addDependency(ConfigurationName.api, lib2)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib2"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib2.Lib2Class

        val clazz2 = Lib2Class()
        """.trimIndent()
      )
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class

        private val clazz = Lib1Class()
        """.trimIndent(),
        "androidTestDebug".asSourceSetName()
      )
    }

    run().isSuccess shouldBe true

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          androidTestDebugImplementation(project(path = ":lib1"))
          api(project(path = ":lib2"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "androidTestDebugImplementation",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `inherited from implementation dependency and part of API with auto-correct should be fixed as api config`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addDependency(ConfigurationName.implementation, lib2)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib2"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        val clazz = Lib1Class()
        private val clazz2 = Lib2Class()
        """.trimIndent()
      )
    }

    run().isSuccess shouldBe true

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          implementation(project(path = ":lib2"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "api",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `deeply inherited from testImplementation dependency with auto-correct should be fixed as testImplementation`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addDependency(ConfigurationName.api, lib2)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib2"))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class

        open class Lib3Class : Lib1Class()
        """.trimIndent()
      )
    }

    val lib4 = kotlinProject(":lib4") {
      addDependency(ConfigurationName.testImplementation, lib3)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(project(path = ":lib3"))
        }
        """
      }
      addKotlinSource(
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

    run().isSuccess shouldBe true

    lib4.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(project(path = ":lib1"))
          testImplementation(project(path = ":lib2"))
          testImplementation(project(path = ":lib3"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "api",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        ),
        unusedDependency(
          fixed = true,
          configuration = "api",
          dependency = ":lib2",
          position = "6, 3"
        )
      ),
      ":lib4" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "testImplementation",
          dependency = ":lib1",
          source = ":lib3",
          position = "6, 3"
        ),
        inheritedDependency(
          fixed = true,
          configuration = "testImplementation",
          dependency = ":lib2",
          source = ":lib3",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `inherited testFixtures and implementation from testFixtures with auto-correct should be fixed as testFixtures via testImplementation`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        interface Lib1Interface
        """.trimIndent()
      )
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """.trimIndent(),
        SourceSetName.TEST_FIXTURES
      )
    }

    val lib2 = kotlinProject(":lib2") {
      hasTestFixturesPlugin = true
      addDependency("testFixturesImplementation".asConfigurationName(), lib1)
      addDependency("testFixturesApi".asConfigurationName(), lib1, asTestFixture = true)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testFixturesImplementation(project(path = ":lib1"))
          testFixturesApi(testFixtures(project(path = ":lib1")))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib1.Lib1Interface

        open class Lib2Class : Lib1Class(), Lib1Interface
        """.trimIndent(),
        SourceSetName.TEST_FIXTURES
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addDependency(ConfigurationName.testImplementation, lib2, asTestFixture = true)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(testFixtures(project(path = ":lib2")))
        }
        """
      }
      addKotlinSource(
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

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testFixturesApi(project(path = ":lib1"))
          // testFixturesImplementation(project(path = ":lib1"))  // ModuleCheck finding [must-be-api]
          testFixturesApi(testFixtures(project(path = ":lib1")))
        }
    """

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(testFixtures(project(path = ":lib1")))
          testImplementation(testFixtures(project(path = ":lib2")))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        mustBeApi(
          fixed = true,
          configuration = "testFixturesImplementation",
          dependency = ":lib1",
          position = "6, 3"
        )
      ),
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "testImplementation",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `inherited testFixtures from testFixtures with auto-correct should be fixed as testFixtures via testImplementation`() {

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
      addDependency("testFixturesApi".asConfigurationName(), lib1, asTestFixture = true)

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
      addDependency(ConfigurationName.testImplementation, lib2, asTestFixture = true)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(testFixtures(project(path = ":lib2")))
        }
        """
      }
      addKotlinSource(
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
          testImplementation(testFixtures(project(path = ":lib2")))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "testImplementation",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `inherited testFixtures from api with auto-correct should be fixed as testFixtures via testImplementation`() {

    val lib1 = kotlinProject(":lib1") {
      hasTestFixturesPlugin = true
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """.trimIndent(),
        SourceSetName.TEST_FIXTURES
      )
    }

    val lib2 = kotlinProject(":lib2") {
      hasTestFixturesPlugin = true
      addDependency("api".asConfigurationName(), lib1, asTestFixture = true)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(testFixtures(project(path = ":lib1")))
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
      addDependency(ConfigurationName.testImplementation, lib2, asTestFixture = true)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(testFixtures(project(path = ":lib2")))
        }
        """
      }
      addKotlinSource(
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

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testFixturesApi(testFixtures(project(path = ":lib1")))
          // api(testFixtures(project(path = ":lib1")))  // ModuleCheck finding [unused-dependency]
        }
    """

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(testFixtures(project(path = ":lib1")))
          testImplementation(testFixtures(project(path = ":lib2")))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        overshot(
          fixed = true,
          configuration = "testFixturesApi",
          dependency = ":lib1",
          position = "6, 3"
        ),
        unusedDependency(
          fixed = true,
          configuration = "api",
          dependency = ":lib1",
          position = "6, 3"
        )
      ),
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "testImplementation",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `inherited main source should be added as non-test-fixtures testImplementation`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """.trimIndent()
      )
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency("testFixturesApi".asConfigurationName(), lib1)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testFixturesApi(project(path = ":lib1"))
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
      addDependency(ConfigurationName.testImplementation, lib2, asTestFixture = true)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(testFixtures(project(path = ":lib2")))
        }
        """
      }
      addKotlinSource(
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

    run().isSuccess shouldBe true

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(project(path = ":lib1"))
          testImplementation(testFixtures(project(path = ":lib2")))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib3" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "testImplementation",
          dependency = ":lib1",
          source = ":lib2",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `inherited main source testFixture in same module with auto-correct should be fixed as normal testImplementation`() {

    val lib1 = kotlinProject(":lib1") {
      hasTestFixturesPlugin = true
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """.trimIndent()
      )
      addKotlinSource(
        """
        package com.modulecheck.lib1.test

        import com.modulecheck.lib1.Lib1Class

        open class FakeLib1Class : Lib1Class()
        """.trimIndent(),
        SourceSetName.TEST_FIXTURES
      )
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.testImplementation, lib1, asTestFixture = true)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(testFixtures(project(path = ":lib1")))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib1.test.FakeLib1Class

        val clazz = Lib1Class()
        private val clazz2 = FakeLib1Class()
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
          testImplementation(testFixtures(project(path = ":lib1")))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "testImplementation",
          dependency = ":lib1",
          source = null,
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `inherited main source testFixture in same module with auto-correct should be fixed as normal api`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """.trimIndent()
      )
      addKotlinSource(
        """
        package com.modulecheck.lib1.test

        import com.modulecheck.lib1.Lib1Class

        open class FakeLib1Class : Lib1Class()
        """.trimIndent(),
        SourceSetName.TEST_FIXTURES
      )
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1, asTestFixture = true)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(testFixtures(project(path = ":lib1")))
        }
        """
      }
      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib1.test.FakeLib1Class

        val clazz = Lib1Class()
        private val clazz2 = FakeLib1Class()
        """.trimIndent()
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          implementation(testFixtures(project(path = ":lib1")))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        inheritedDependency(
          fixed = true,
          configuration = "api",
          dependency = ":lib1",
          source = null,
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `not inherited when only used in tests and already declared as testImplementation`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """.trimIndent()
      )
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.testImplementation, lib1)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(project(path = ":lib1"))
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

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(project(path = ":lib1"))
        }
    """

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `not inherited when exposed as api but used in tests and already declared as testImplementation`() {

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
        """.trimIndent()
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addDependency(ConfigurationName.testImplementation, lib1)
      addDependency(ConfigurationName.testImplementation, lib2)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(project(path = ":lib1"))
          testImplementation(project(path = ":lib2"))
        }
        """
      }
      addKotlinSource(
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

    run().isSuccess shouldBe true

    lib3.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          testImplementation(project(path = ":lib1"))
          testImplementation(project(path = ":lib2"))
        }
    """

    logger.parsedReport() shouldBe listOf()
  }
}
