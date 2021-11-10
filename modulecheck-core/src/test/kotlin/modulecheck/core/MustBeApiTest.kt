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
import org.junit.jupiter.api.Test

class MustBeApiTest : ProjectTest() {

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
  fun `public property from implementation without auto-correct should fail`() {

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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name         source    build file
                X  :lib1         mustBeApi              /lib2/build.gradle.kts: (6, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `public generic property from implementation without auto-correct should fail`() {

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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name         source    build file
                X  :lib1         mustBeApi              /lib2/build.gradle.kts: (6, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `public property from implementation with auto-correct should be fixed`() {

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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name         source    build file
                ✔  :lib1         mustBeApi              /lib2/build.gradle.kts: (6, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `private property from implementation with auto-correct should not be changed`() {

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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `private property from implementation inside public class with auto-correct should not be changed`() {

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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `internal property from implementation with auto-correct should not be changed`() {

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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `public property from dependency in test source should not require API`() {

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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `internal property in class from implementation with auto-correct should not be changed`() {

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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `supertype from implementation with auto-correct should be fixed`() {

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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name         source    build file
                ✔  :lib1         mustBeApi              /lib2/build.gradle.kts: (6, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `auto-correct should only replace the configuration invocation text`() {

    val runner = ModuleCheckRunner(
      autoCorrect = true,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency         name         source    build file
                ✔  :implementation    mustBeApi              /lib2/build.gradle.kts: (8, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `supertype of internal class from implementation with auto-correct should not be changed`() {

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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `public return type from implementation with auto-correct should be fixed`() {

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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name         source    build file
                ✔  :lib1         mustBeApi              /lib2/build.gradle.kts: (6, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `internal return type from implementation with auto-correct should not be changed`() {

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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """ModuleCheck found 0 issues"""
  }

  @Test
  fun `public argument type from implementation with auto-correct should be fixed`() {

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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name         source    build file
                ✔  :lib1         mustBeApi              /lib2/build.gradle.kts: (6, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `public type argument from implementation with auto-correct should be fixed`() {

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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name         source    build file
                ✔  :lib1         mustBeApi              /lib2/build.gradle.kts: (6, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `public generic bound type from implementation with auto-correct should be fixed`() {

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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name         source    build file
                ✔  :lib1         mustBeApi              /lib2/build.gradle.kts: (6, 3):

        ModuleCheck found 1 issue
        """
  }

  @Test
  fun `two public public properties from implementation with auto-correct should be fixed`() {

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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name         source    build file
                ✔  :lib1         mustBeApi              /lib2/build.gradle.kts: (6, 3):
                ✔  :lib3         mustBeApi              /lib2/build.gradle.kts: (7, 3):

        ModuleCheck found 2 issues
        """
  }

  @Test
  fun `two public supertypes from implementation with auto-correct should be fixed`() {

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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name         source    build file
                ✔  :lib1         mustBeApi              /lib2/build.gradle.kts: (6, 3):
                ✔  :lib3         mustBeApi              /lib2/build.gradle.kts: (7, 3):

        ModuleCheck found 2 issues
        """
  }

  @Test
  fun `two public return types from implementation with auto-correct should be fixed`() {

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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name         source    build file
                ✔  :lib1         mustBeApi              /lib2/build.gradle.kts: (6, 3):
                ✔  :lib3         mustBeApi              /lib2/build.gradle.kts: (7, 3):

        ModuleCheck found 2 issues
        """
  }

  @Test
  fun `two public argument types from implementation with auto-correct should be fixed`() {

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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name         source    build file
                ✔  :lib1         mustBeApi              /lib2/build.gradle.kts: (6, 3):
                ✔  :lib3         mustBeApi              /lib2/build.gradle.kts: (7, 3):

        ModuleCheck found 2 issues
        """
  }

  @Test
  fun `two public type arguments from implementation with auto-correct should be fixed`() {

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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name         source    build file
                ✔  :lib1         mustBeApi              /lib2/build.gradle.kts: (6, 3):
                ✔  :lib3         mustBeApi              /lib2/build.gradle.kts: (7, 3):

        ModuleCheck found 2 issues
        """
  }

  @Test
  fun `two public generic bound types from implementation with auto-correct should be fixed`() {

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

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
            :lib2
                   dependency    name         source    build file
                ✔  :lib1         mustBeApi              /lib2/build.gradle.kts: (6, 3):
                ✔  :lib3         mustBeApi              /lib2/build.gradle.kts: (7, 3):

        ModuleCheck found 2 issues
        """
  }
}
