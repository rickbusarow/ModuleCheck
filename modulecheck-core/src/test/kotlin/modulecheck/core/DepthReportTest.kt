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

import modulecheck.api.test.TestSettings
import modulecheck.core.rule.DepthRule
import modulecheck.core.rule.MultiRuleFindingFactory
import modulecheck.core.rule.SingleRuleFindingFactory
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.parsing.gradle.SourceSet
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.runtime.test.RunnerTest
import modulecheck.testing.createSafely
import modulecheck.utils.remove
import org.junit.jupiter.api.Test
import java.io.File

internal class DepthReportTest : RunnerTest() {

  override val settings by resets {
    TestSettings().apply {
      reports.depths.outputPath = File(testProjectDir, reports.depths.outputPath).path
    }
  }
  override val findingFactory by resets { SingleRuleFindingFactory(DepthRule()) }

  val outputFile by resets { File(settings.reports.depths.outputPath) }

  @Test
  fun `depth report should not be created if disabled in settings`() {

    settings.reports.depths.enabled = false

    val runner = runner(
      autoCorrect = false,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1")

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    project(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    val result = runner.run(allProjects())

    result.isSuccess shouldBe true

    outputFile.exists() shouldBe false
  }

  @Test
  fun `depth report should be created if enabled in settings`() {

    settings.reports.depths.enabled = true

    val runner = runner(
      autoCorrect = false,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1")

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    project(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    val result = runner.run(allProjects())

    result.isSuccess shouldBe true

    outputFile.readText() shouldBe """
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

    val runner = runner(
      autoCorrect = false,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {

      val myFile = File(projectDir, "src/main/kotlin/MyFile.kt").createSafely()

      sourceSets[SourceSetName.MAIN] = SourceSet(
        name = SourceSetName.MAIN,
        jvmFiles = setOf(myFile)
      )
    }

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    project(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    val result = runner.run(allProjects())

    result.isSuccess shouldBe true

    outputFile.readText() shouldBe """
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

    val runner = runner(
      autoCorrect = false,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      sourceSets[SourceSetName.MAIN] = SourceSet(name = SourceSetName.MAIN)
    }

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    project(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    val result = runner.run(allProjects())

    result.isSuccess shouldBe true

    outputFile.readText() shouldBe """
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

    val runner = runner(
      autoCorrect = false,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1") {
      addSourceSet(SourceSetName.TEST)
    }

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    project(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    lib1.addDependency(ConfigurationName("testImplementation"), lib2)

    val result = runner.run(allProjects())

    result.isSuccess shouldBe true

    outputFile.readText() shouldBe """
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

    val runner = runner(
      autoCorrect = true,
      findingFactory = MultiRuleFindingFactory(
        settings,
        ruleFactory.create(settings)
      )
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
      addDependency(ConfigurationName.implementation, lib1)

      addSource(
        "com/modulecheck/lib2/Lib2Class.kt",
        """
        package com.modulecheck.lib2

        class Lib2Class
        """.trimIndent()
      )

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

    project(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)

      addSource(
        "com/modulecheck/app/App.kt",
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

    runner.run(allProjects()).isSuccess shouldBe true

    logger.collectReport()
      .joinToString()
      .clean()
      .remove("\u200B") shouldBe """
          :lib2
                 configuration     dependency    name                source    build file
              ✔  implementation    :lib1         unusedDependency              /lib2/build.gradle.kts: (6, 3):

      -- ModuleCheck main source set depth results --
          depth    modules
          0        [:lib1, :lib2]
          1        [:app]

      ModuleCheck found 1 issue
    """

    outputFile.readText() shouldBe """
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

    val runner = runner(
      autoCorrect = false,
      findingFactory = findingFactory
    )

    val lib1 = project(":lib1")
    val debug1 = project(":debug1") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    val lib2 = project(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }
    val debug2 = project(":debug2") {
      addDependency(ConfigurationName.implementation, debug1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    project(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
      addSourceSet(SourceSetName("debug"))
      addDependency(ConfigurationName("debugImplementation"), debug2)
    }

    val result = runner.run(allProjects())

    result.isSuccess shouldBe true

    outputFile.readText() shouldBe """
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
