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

import modulecheck.config.fake.TestSettings
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.sourceset.SourceSetName
import modulecheck.project.generation.maybeAddSourceSet
import modulecheck.rule.impl.DepthRule
import modulecheck.rule.impl.FindingFactoryImpl
import modulecheck.rule.impl.UnusedDependencyRule
import modulecheck.runtime.test.RunnerTest
import modulecheck.utils.remove
import org.junit.jupiter.api.Test
import java.io.File

internal class DepthReportTest : RunnerTest() {

  override val settings by resets {
    TestSettings().apply {
      reports.depths.outputPath = File(testProjectDir, reports.depths.outputPath).path
    }
  }

  override val rules = listOf(DepthRule())

  val outputFile by resets { File(settings.reports.depths.outputPath) }

  @Test
  fun `depth report should not be created if disabled in settings`() {

    settings.reports.depths.enabled = false

    val lib1 = kotlinProject(":lib1")

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    kotlinProject(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    run(autoCorrect = false).isSuccess shouldBe true

    outputFile.exists() shouldBe false
  }

  @Test
  fun `depth report should be created if enabled in settings`() {

    settings.reports.depths.enabled = true

    val lib1 = kotlinProject(":lib1")

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    kotlinProject(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    run(autoCorrect = false).isSuccess shouldBe true

    outputFile shouldHaveText """
      -- ModuleCheck Depth results --

      :app
          source set      depth    most expensive dependencies
          main            2        [:lib2]

      :lib2
          source set      depth    most expensive dependencies
          main            1        [:lib1]

    """
  }

  @Test
  fun `depth report should include zero-depth source sets if they're not empty`() {

    settings.reports.depths.enabled = true

    val lib1 = kotlinProject(":lib1") {
      addKotlinSource("", directory = "com/test", fileName = "MyFile.kt")
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    kotlinProject(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    run(autoCorrect = false).isSuccess shouldBe true

    outputFile shouldHaveText """
      -- ModuleCheck Depth results --

      :app
          source set      depth    most expensive dependencies
          main            2        [:lib2]

      :lib1
          source set      depth    most expensive dependencies
          main            0        []

      :lib2
          source set      depth    most expensive dependencies
          main            1        [:lib1]

    """
  }

  @Test
  fun `depth report should not include zero-depth source sets if they have no files`() {

    settings.reports.depths.enabled = true

    val lib1 = kotlinProject(":lib1")

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    kotlinProject(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    run(autoCorrect = false).isSuccess shouldBe true

    outputFile shouldHaveText """
      -- ModuleCheck Depth results --

      :app
          source set      depth    most expensive dependencies
          main            2        [:lib2]

      :lib2
          source set      depth    most expensive dependencies
          main            1        [:lib1]

    """
  }

  @Test
  fun `test source set depths should use the main depth of the dependency`() {

    settings.reports.depths.enabled = true

    val lib1 = kotlinProject(":lib1") {
      maybeAddSourceSet(SourceSetName.TEST)
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    kotlinProject(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    lib1.addDependency(ConfigurationName("testImplementation"), lib2)

    run(autoCorrect = false).isSuccess shouldBe true

    outputFile shouldHaveText """
      -- ModuleCheck Depth results --

      :app
          source set      depth    most expensive dependencies
          main            2        [:lib2]

      :lib1
          source set      depth    most expensive dependencies
          test            2        [:lib2]

      :lib2
          source set      depth    most expensive dependencies
          main            1        [:lib1]

    """
  }

  @Test
  fun `depth report should be calculated after fixes are applied`() {

    settings.checks.depths = true
    settings.reports.depths.enabled = true

    val lib1 = kotlinProject(":lib1") {

      addKotlinSource(
        """
        package com.modulecheck.lib1

        class Lib1Class
        """.trimIndent()
      )
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)

      addKotlinSource(
        """
        package com.modulecheck.lib2

        class Lib2Class
        """.trimIndent()
      )

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
    }

    kotlinProject(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)

      addKotlinSource(
        """
        package com.modulecheck.app

        import com.modulecheck.lib1.Lib1Class
        import com.modulecheck.lib2.Lib2Class

        class App {
          private val lib1Class = Lib1Class()
          private val lib2Class = Lib2Class()
        }
        """.trimIndent()
      )
    }

    run(
      findingFactory = FindingFactoryImpl(
        listOf(DepthRule(), UnusedDependencyRule(settings))
      ),
      rules = listOf(DepthRule(), UnusedDependencyRule(settings))
    ).isSuccess shouldBe true

    logger.collectReport()
      .joinToString()
      .clean()
      .remove("\u200B") shouldBe """
          :lib2
                 configuration     dependency    name                 source    build file
              ✔  implementation    :lib1         unused-dependency              /lib2/build.gradle.kts: (6, 3):

      -- ModuleCheck main source set depth results --
          depth    modules
          0        [:lib1, :lib2]
          1        [:app]

      ModuleCheck found 1 issue
    """

    outputFile shouldHaveText """
      -- ModuleCheck Depth results --

      :app
          source set      depth    most expensive dependencies
          main            1        [:lib1, :lib2]

      :lib1
          source set      depth    most expensive dependencies
          main            0        []

      :lib2
          source set      depth    most expensive dependencies
          main            0        []

    """
  }

  @Test
  fun `debug source set depth should be reported`() {

    settings.reports.depths.enabled = true

    val lib1 = kotlinProject(":lib1")
    val debug1 = kotlinProject(":debug1") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }
    val debug2 = kotlinProject(":debug2") {
      addDependency(ConfigurationName.implementation, debug1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    kotlinProject(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
      addSourceSet(SourceSetName("debug"))
      addDependency(ConfigurationName("debugImplementation"), debug2)
    }

    run(autoCorrect = false).isSuccess shouldBe true

    outputFile shouldHaveText """
      -- ModuleCheck Depth results --

      :app
          source set      depth    most expensive dependencies
          debug           3        [:debug2]
          main            2        [:lib2]

      :debug1
          source set      depth    most expensive dependencies
          main            1        [:lib1]

      :debug2
          source set      depth    most expensive dependencies
          main            2        [:debug1, :lib2]

      :lib2
          source set      depth    most expensive dependencies
          main            1        [:lib1]

    """
  }
}
