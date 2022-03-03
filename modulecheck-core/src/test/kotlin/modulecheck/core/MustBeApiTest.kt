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
import modulecheck.runtime.test.ProjectFindingReport.mustBeApi
import modulecheck.runtime.test.RunnerTest
import org.junit.jupiter.api.Test

class MustBeApiTest : RunnerTest() {

  @Test
  fun `public property from implementation without auto-correct should fail`() {

    val runner = runner(
      autoCorrect = false,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        val lib1Class = Lib1Class()
        """
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

    val runner = runner(
      autoCorrect = false,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class<T>
        """
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
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        val lib1Class = Lib1Class<String>()
        """
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

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        val lib1Class = Lib1Class()
        """
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
  fun `private property from implementation with auto-correct should not be changed`() {

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        private val lib1Class = Lib1Class()
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
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

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        class Lib2Class {
          private val lib1Class = Lib1Class()
        }
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
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

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        internal val lib1Class = Lib1Class()
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
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
  fun `public property from dependency in test source should not require API`() {

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        val lib1Class = Lib1Class()
        """,
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

    logger.parsedReport() shouldBe listOf()
  }

  @Test
  fun `internal property in class from implementation with auto-correct should not be changed`() {

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        class Lib2Class {
          internal val lib1Class = Lib1Class()
        }
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
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

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """
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
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        class Lib2Class : Lib1Class()
        """
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
  fun `auto-correct should only replace the configuration invocation text`() {

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":implementation") {
      addSource(
        "com/modulecheck/implementation/Lib1Class.kt",
        """
        package com.modulecheck.implementation

        open class Lib1Class
        """
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
          // this module dependency is an implementation
          // implementation can be the beginning of the comment
          implementation(project(path = ":implementation")) // it's an implementation
        }
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.implementation.Lib1Class

        class Lib2Class : Lib1Class()
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
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

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """
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
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        internal class Lib2Class : Lib1Class()
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
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

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        fun lib1Class(): Lib1Class = Lib1Class()
        """
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

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        internal fun lib1Class(): Lib1Class = Lib1Class()
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
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

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        fun bindLib1(clazz: Lib1Class): Lib1Class = clazz
        """
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

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        fun foo(t: List<Lib1Class>) = Unit
        """
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

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class

        fun <T: Lib1Class> foo(t: T) = Unit
        """
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

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
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
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        fun <T: com.modulecheck.lib1.Lib1Class> foo(t: T) = Unit
        """
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

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
      )
    }

    val lib3 = project(":lib3") {
      addSource(
        "com/modulecheck/lib3/Lib3Class.kt",
        """
        package com.modulecheck.lib3

        class Lib3Class
        """
      )
    }

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib3)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
          implementation(project(path = ":lib3"))
        }
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib3.Lib3Class

        val lib1Class = Lib1Class()
        val lib3Class = Lib3Class()
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          api(project(path = ":lib3"))
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

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        open class Lib1Class
        """
      )
    }

    val lib3 = project(":lib3") {
      addSource(
        "com/modulecheck/lib3/Lib3Class.kt",
        """
        package com.modulecheck.lib3

        interface Lib3Interface
        """
      )
    }

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib3)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
          implementation(project(path = ":lib3"))
        }
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib3.Lib3Interface

        class Lib2Class : Lib1Class(), Lib3Interface
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          api(project(path = ":lib3"))
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

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
      )
    }

    val lib3 = project(":lib3") {
      addSource(
        "com/modulecheck/lib3/Lib3Class.kt",
        """
        package com.modulecheck.lib3

        class Lib3Class
        """
      )
    }

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib3)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
          implementation(project(path = ":lib3"))
        }
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib3.Lib3Class

        fun lib1Class(): Lib1Class = Lib1Class()
        fun lib3Class(): Lib3Class = Lib3Class()
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          api(project(path = ":lib3"))
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

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
      )
    }

    val lib3 = project(":lib3") {
      addSource(
        "com/modulecheck/lib3/Lib3Class.kt",
        """
        package com.modulecheck.lib3

        class Lib3Class
        """
      )
    }

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib3)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
          implementation(project(path = ":lib3"))
        }
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib3.Lib3Class

        fun bindLib1(clazz: Lib1Class): Lib1Class = clazz
        fun bindLib3(clazz: Lib3Class): Lib3Class = clazz
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          api(project(path = ":lib3"))
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

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
      )
    }

    val lib3 = project(":lib3") {
      addSource(
        "com/modulecheck/lib3/Lib3Class.kt",
        """
        package com.modulecheck.lib3

        class Lib3Class
        """
      )
    }

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib3)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
          implementation(project(path = ":lib3"))
        }
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib3.Lib3Class

        fun foo(lib1s: List<Lib1Class>, lib3Comparator: Comparator<Lib3Class>) = Unit
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          api(project(path = ":lib3"))
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

    val runner = runner(
      autoCorrect = true,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSource(
        "com/modulecheck/lib1/Lib1Class.kt",
        """
        package com.modulecheck.lib1

        class Lib1Class
        """
      )
    }

    val lib3 = project(":lib3") {
      addSource(
        "com/modulecheck/lib3/Lib3Class.kt",
        """
        package com.modulecheck.lib3

        class Lib3Class
        """
      )
    }

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib3)

      buildFile.writeText(
        """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          implementation(project(path = ":lib1"))
          implementation(project(path = ":lib3"))
        }
        """
      )

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib3.Lib3Class

        fun <T : Lib1Class, R : Lib3Class> foo(t: T): R = TODO()
        """
      )
    }

    runner.run(allProjects()).isSuccess shouldBe true

    lib2.buildFile.readText() shouldBe """
        plugins {
          kotlin("jvm")
        }

        dependencies {
          api(project(path = ":lib1"))
          api(project(path = ":lib3"))
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
