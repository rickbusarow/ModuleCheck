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
import modulecheck.model.sourceset.SourceSetName
import modulecheck.runtime.test.ProjectFindingReport.mustBeApi
import modulecheck.runtime.test.RunnerTest
import modulecheck.utils.child
import modulecheck.utils.createSafely
import org.junit.jupiter.api.Test

class MustBeApiTest : RunnerTest() {

  @Test
  fun `public property from implementation without auto-correct should fail`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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

        val lib1Class = Lib1Class()
        """
      )
    }

    run(
      autoCorrect = false
    ).isSuccess shouldBe false

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
        mustBeApi(
          fixed = false,
          configuration = "implementation",
          dependency = ":lib1",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `public generic property from implementation without auto-correct should fail`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class<T>
        """
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

        val lib1Class = Lib1Class<String>()
        """
      )
    }

    run(
      autoCorrect = false
    ).isSuccess shouldBe false

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
        mustBeApi(
          fixed = false,
          configuration = "implementation",
          dependency = ":lib1",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `public property from implementation with auto-correct should be fixed`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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

        val lib1Class = Lib1Class()
        """
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [must-be-api]
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          position = "6, 3"
        )
      )
    )
  }

  // https://github.com/RBusarow/ModuleCheck/issues/443
  @Test
  fun `switching to api dependency after a blank line should preserve all newlines -- kotlin`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
      )
    }

    val lib2 = kotlinProject(":lib2") {

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

        class Lib2Class
        """
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
        """
      )
    }

    val lib4 = kotlinProject(":lib4") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
      addDependency(ConfigurationName.implementation, lib3)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))

          implementation(project(path = ":lib2"))
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

        val lib1Class = Lib1Class()
        val lib2Class = Lib2Class()
        val lib3Class = Lib3Class()
        """
      )
    }

    run().isSuccess shouldBe true

    lib4.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [must-be-api]

          api(project(path = ":lib2"))
          // implementation(project(path = ":lib2"))  // ModuleCheck finding [must-be-api]
          api(project(path = ":lib3"))
          // implementation(project(path = ":lib3"))  // ModuleCheck finding [must-be-api]
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib4" to listOf(
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          position = "6, 3"
        ),
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib2",
          position = "8, 3"
        ),
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib3",
          position = "9, 3"
        )
      )
    )
  }

  // https://github.com/RBusarow/ModuleCheck/issues/443
  @Test
  fun `switching to api dependency after a blank line should preserve all newlines -- groovy`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
      )
    }

    val lib2 = kotlinProject(":lib2") {

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

        class Lib2Class
        """
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
        """
      )
    }

    val lib4 = kotlinProject(":lib4") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
      addDependency(ConfigurationName.implementation, lib3)

      buildFile.delete()
      buildFile = projectDir.child("build.gradle")
        .createSafely(
          """
          plugins {
            id 'org.jetbrains.kotlin.jvm' version '1.6.10'
          }

          dependencies {
            implementation project(':lib1')

            implementation project(':lib2')
            implementation project(':lib3')
          }
          """.trimIndent()
        )

      addKotlinSource(
        """
        package com.modulecheck.lib4

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class
        import com.modulecheck.lib3.Lib3Class

        val lib1Class = Lib1Class()
        val lib2Class = Lib2Class()
        val lib3Class = Lib3Class()
        """
      )
    }

    run().isSuccess shouldBe true

    lib4.buildFile shouldHaveText """
        plugins {
          id 'org.jetbrains.kotlin.jvm' version '1.6.10'
        }

        dependencies {
          api project(':lib1')
          // implementation project(':lib1')  // ModuleCheck finding [must-be-api]

          api project(':lib2')
          // implementation project(':lib2')  // ModuleCheck finding [must-be-api]
          api project(':lib3')
          // implementation project(':lib3')  // ModuleCheck finding [must-be-api]
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib4" to listOf(
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          position = "6, 3"
        ),
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib2",
          position = "8, 3"
        ),
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib3",
          position = "9, 3"
        )
      )
    )
  }

  @Test
  fun `private property from implementation with auto-correct should not be changed`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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

        private val lib1Class = Lib1Class()
        """
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
        }
    """

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `private property from implementation inside public class with auto-correct should not be changed`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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

        class Lib2Class {
          private val lib1Class = Lib1Class()
        }
        """
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
        }
    """

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `internal property from implementation with auto-correct should not be changed`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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

        internal val lib1Class = Lib1Class()
        """
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
        }
    """

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `api and implementation of the same dependency with auto-correct should not be changed`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
      )
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.api, lib1)
      addDependency(ConfigurationName.implementation, lib1)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          implementation(project(path = ":lib1"))
        }
        """
      }

      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        val lib1Class = Lib1Class()
        """
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          implementation(project(path = ":lib1"))
        }
    """

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `public property from dependency in test source should not require API`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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

        val lib1Class = Lib1Class()
        """,
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
  fun `internal property in class from implementation with auto-correct should not be changed`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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

        class Lib2Class {
          internal val lib1Class = Lib1Class()
        }
        """
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
        }
    """

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `supertype from implementation with auto-correct should be fixed`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """
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

        class Lib2Class : Lib1Class()
        """
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [must-be-api]
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `must be api from invisible dependency with unrelated api dependency declaration`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """
      )
    }

    val lib2 = kotlinProject(":lib2") {

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

        class Lib2Class
        """
      )
    }

    val lib3 = kotlinProject(":lib3") {
      // lib1 is added as a dependency, but it's not in the build file.
      // This is intentional, because it mimics the behavior of a convention plugin
      // which adds a dependency without any visible declaration in the build file
      addDependency(ConfigurationName.implementation, lib1, addToBuildFile = false)
      addDependency(ConfigurationName.api, lib2, addToBuildFile = false)

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

        class Lib3Class : Lib1Class()
        val lib2Class = Lib2Class()
        """
      )
    }

    run().isSuccess shouldBe false

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
        mustBeApi(
          fixed = false,
          configuration = "implementation",
          dependency = ":lib1",
          position = null
        )
      )
    )
  }

  @Test
  fun `must be api from invisible dependency with unrelated implementation dependency declaration`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """
      )
    }

    val lib2 = kotlinProject(":lib2") {

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

        class Lib2Class
        """
      )
    }

    val lib3 = kotlinProject(":lib3") {
      // lib1 is added as a dependency, but it's not in the build file.
      // This is intentional, because it mimics the behavior of a convention plugin
      // which adds a dependency without any visible declaration in the build file
      addDependency(ConfigurationName.implementation, lib1, addToBuildFile = false)
      addDependency(ConfigurationName.implementation, lib2, addToBuildFile = false)

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

        class Lib3Class : Lib1Class()
        private val lib2Class = Lib2Class()
        """
      )
    }

    run().isSuccess shouldBe false

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
        mustBeApi(
          fixed = false,
          configuration = "implementation",
          dependency = ":lib1",
          position = null
        )
      )
    )
  }

  @Test
  fun `must be api from invisible dependency with unrelated implementation external dependency`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """
      )
    }

    val lib2 = kotlinProject(":lib2") {
      // lib1 is added as a dependency, but it's not in the build file.
      // This is intentional, because it mimics the behavior of a convention plugin
      // which adds a dependency without any visible declaration in the build file
      addDependency(ConfigurationName.implementation, lib1, addToBuildFile = false)

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
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        class Lib3Class : Lib1Class()
        private val lib2Class = Lib2Class()
        """
      )
    }

    run().isSuccess shouldBe false

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(":lib1"))

          implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        mustBeApi(
          fixed = false,
          configuration = "implementation",
          dependency = ":lib1",
          position = null
        )
      )
    )
  }

  @Test
  fun `must be api from invisible dependency with unrelated api external dependency`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """
      )
    }

    val lib2 = kotlinProject(":lib2") {
      // lib1 is added as a dependency, but it's not in the build file.
      // This is intentional, because it mimics the behavior of a convention plugin
      // which adds a dependency without any visible declaration in the build file
      addDependency(ConfigurationName.implementation, lib1, addToBuildFile = false)

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
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        class Lib3Class : Lib1Class()
        private val lib2Class = Lib2Class()
        """
      )
    }

    run().isSuccess shouldBe false

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(":lib1"))

          api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        mustBeApi(
          fixed = false,
          configuration = "implementation",
          dependency = ":lib1",
          position = null
        )
      )
    )
  }

  @Test
  fun `must be api from invisible dependency with empty multi-line dependencies block`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """
      )
    }

    val lib2 = kotlinProject(":lib2") {
      // lib1 is added as a dependency, but it's not in the build file.
      // This is intentional, because it mimics the behavior of a convention plugin
      // which adds a dependency without any visible declaration in the build file
      addDependency(ConfigurationName.implementation, lib1, addToBuildFile = false)

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
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        class Lib3Class : Lib1Class()
        private val lib2Class = Lib2Class()
        """
      )
    }

    run().isSuccess shouldBe false

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(":lib1"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        mustBeApi(
          fixed = false,
          configuration = "implementation",
          dependency = ":lib1",
          position = null
        )
      )
    )
  }

  @Test
  fun `must be api from invisible dependency with empty single-line dependencies block`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """
      )
    }

    val lib2 = kotlinProject(":lib2") {
      // lib1 is added as a dependency, but it's not in the build file.
      // This is intentional, because it mimics the behavior of a convention plugin
      // which adds a dependency without any visible declaration in the build file
      addDependency(ConfigurationName.implementation, lib1, addToBuildFile = false)

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
        package com.modulecheck.lib3

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        class Lib3Class : Lib1Class()
        private val lib2Class = Lib2Class()
        """
      )
    }

    run().isSuccess shouldBe false

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(":lib1"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        mustBeApi(
          fixed = false,
          configuration = "implementation",
          dependency = ":lib1",
          position = null
        )
      )
    )
  }

  @Test
  fun `must be api from invisible dependency with no dependencies block`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """
      )
    }

    val lib2 = kotlinProject(":lib2") {
      // lib1 is added as a dependency, but it's not in the build file.
      // This is intentional, because it mimics the behavior of a convention plugin
      // which adds a dependency without any visible declaration in the build file
      addDependency(ConfigurationName.implementation, lib1, addToBuildFile = false)

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

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        class Lib3Class : Lib1Class()
        private val lib2Class = Lib2Class()
        """
      )
    }

    run().isSuccess shouldBe false

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(":lib1"))
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        mustBeApi(
          fixed = false,
          configuration = "implementation",
          dependency = ":lib1",
          position = null
        )
      )
    )
  }

  @Test
  fun `auto-correct should only replace the configuration invocation text`() {

    settings.deleteUnused = true

    val lib1 = kotlinProject(":implementation") {
      addKotlinSource(
        """
        package com.modulecheck.implementation

        open class Lib1Class
        """
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
          // this module dependency is an implementation
          // implementation can be the beginning of the comment
          implementation(project(path = ":implementation")) // it's an implementation
        }
        """
      }

      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.implementation.Lib1Class

        class Lib2Class : Lib1Class()
        """
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          // this module dependency is an implementation
          // implementation can be the beginning of the comment
          api(project(path = ":implementation")) // it's an implementation
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":implementation",
          position = "8, 3"
        )
      )
    )
  }

  @Test
  fun `supertype of internal class from implementation with auto-correct should not be changed`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """
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

        internal class Lib2Class : Lib1Class()
        """
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
        }
    """

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `public return type from implementation with auto-correct should be fixed`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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

        fun lib1Class(): Lib1Class = Lib1Class()
        """
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [must-be-api]
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `internal return type from implementation with auto-correct should not be changed`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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

        internal fun lib1Class(): Lib1Class = Lib1Class()
        """
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
        }
    """

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `public argument type from implementation with auto-correct should be fixed`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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

        fun bindLib1(clazz: Lib1Class): Lib1Class = clazz
        """
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [must-be-api]
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `public type argument from implementation with auto-correct should be fixed`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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

        fun foo(t: List<Lib1Class>) = Unit
        """
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [must-be-api]
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `public generic bound type from implementation with auto-correct should be fixed`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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

        fun <T: Lib1Class> foo(t: T) = Unit
        """
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [must-be-api]
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `public generic fully qualified bound type from implementation with auto-correct should be fixed`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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

        fun <T: com.modulecheck.lib1.Lib1Class> foo(t: T) = Unit
        """
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [must-be-api]
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `public generic wrapping type should count as api reference`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class<T>
        """
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

        val lib1ClassOfString: Lib1Class<String> = TODO()
        """
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [must-be-api]
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          position = "6, 3"
        )
      )
    )
  }

  @Test
  fun `two public public properties from implementation with auto-correct should be fixed`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addKotlinSource(
        """
        package com.modulecheck.lib3

        class Lib3Class
        """
      )
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib3)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
          implementation(project(path = ":lib3"))
        }
        """
      }

      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib3.Lib3Class

        val lib1Class = Lib1Class()
        val lib3Class = Lib3Class()
        """
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [must-be-api]
          api(project(path = ":lib3"))
          // implementation(project(path = ":lib3"))  // ModuleCheck finding [must-be-api]
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          position = "6, 3"
        ),
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib3",
          position = "7, 3"
        )
      )
    )
  }

  @Test
  fun `two public supertypes from implementation with auto-correct should be fixed`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addKotlinSource(
        """
        package com.modulecheck.lib3

        interface Lib3Interface
        """
      )
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib3)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
          implementation(project(path = ":lib3"))
        }
        """
      }

      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib3.Lib3Interface

        class Lib2Class : Lib1Class(), Lib3Interface
        """
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [must-be-api]
          api(project(path = ":lib3"))
          // implementation(project(path = ":lib3"))  // ModuleCheck finding [must-be-api]
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          position = "6, 3"
        ),
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib3",
          position = "7, 3"
        )
      )
    )
  }

  @Test
  fun `two public return types from implementation with auto-correct should be fixed`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addKotlinSource(
        """
        package com.modulecheck.lib3

        class Lib3Class
        """
      )
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib3)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
          implementation(project(path = ":lib3"))
        }
        """
      }

      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib3.Lib3Class

        fun lib1Class(): Lib1Class = Lib1Class()
        fun lib3Class(): Lib3Class = Lib3Class()
        """
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [must-be-api]
          api(project(path = ":lib3"))
          // implementation(project(path = ":lib3"))  // ModuleCheck finding [must-be-api]
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          position = "6, 3"
        ),
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib3",
          position = "7, 3"
        )
      )
    )
  }

  @Test
  fun `two public argument types from implementation with auto-correct should be fixed`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addKotlinSource(
        """
        package com.modulecheck.lib3

        class Lib3Class
        """
      )
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib3)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
          implementation(project(path = ":lib3"))
        }
        """
      }

      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib3.Lib3Class

        fun bindLib1(clazz: Lib1Class): Lib1Class = clazz
        fun bindLib3(clazz: Lib3Class): Lib3Class = clazz
        """
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [must-be-api]
          api(project(path = ":lib3"))
          // implementation(project(path = ":lib3"))  // ModuleCheck finding [must-be-api]
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          position = "6, 3"
        ),
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib3",
          position = "7, 3"
        )
      )
    )
  }

  @Test
  fun `two public type arguments from implementation with auto-correct should be fixed`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addKotlinSource(
        """
        package com.modulecheck.lib3

        class Lib3Class
        """
      )
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib3)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
          implementation(project(path = ":lib3"))
        }
        """
      }

      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib3.Lib3Class

        fun foo(lib1s: List<Lib1Class>, lib3Comparator: Comparator<Lib3Class>) = Unit
        """
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [must-be-api]
          api(project(path = ":lib3"))
          // implementation(project(path = ":lib3"))  // ModuleCheck finding [must-be-api]
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          position = "6, 3"
        ),
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib3",
          position = "7, 3"
        )
      )
    )
  }

  @Test
  fun `two public generic bound types from implementation with auto-correct should be fixed`() {

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
      )
    }

    val lib3 = kotlinProject(":lib3") {
      addKotlinSource(
        """
        package com.modulecheck.lib3

        class Lib3Class
        """
      )
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib3)

      buildFile {
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
          implementation(project(path = ":lib3"))
        }
        """
      }

      addKotlinSource(
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib3.Lib3Class

        fun <T : Lib1Class, R : Lib3Class> foo(t: T): R = TODO()
        """
      )
    }

    run().isSuccess shouldBe true

    lib2.buildFile shouldHaveText """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          // implementation(project(path = ":lib1"))  // ModuleCheck finding [must-be-api]
          api(project(path = ":lib3"))
          // implementation(project(path = ":lib3"))  // ModuleCheck finding [must-be-api]
        }
    """

    logger.parsedReport() shouldBe listOf(
      ":lib2" to listOf(
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib1",
          position = "6, 3"
        ),
        mustBeApi(
          fixed = true,
          configuration = "implementation",
          dependency = ":lib3",
          position = "7, 3"
        )
      )
    )
  }
}
