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

import io.kotest.matchers.file.shouldNotExist
import modulecheck.config.ModuleCheckSettings
import modulecheck.config.fake.TestChecksSettings
import modulecheck.config.fake.TestSettings
import modulecheck.model.dependency.ConfigurationName
import modulecheck.rule.impl.FindingFactoryImpl
import modulecheck.runtime.test.RunnerTest
import modulecheck.runtime.test.RunnerTestEnvironment
import modulecheck.testing.SkipInStackTrace
import modulecheck.testing.asContainers
import modulecheck.utils.remove
import modulecheck.utils.resolve
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.util.stream.Stream

internal class DepthOutputTest : RunnerTest() {

  override val settings: RunnerTestEnvironment.() -> ModuleCheckSettings = {
    TestSettings(
      checks = TestChecksSettings(
        redundantDependency = false,
        unusedDependency = false,
        overShotDependency = false,
        mustBeApi = false,
        inheritedDependency = false,
        sortDependencies = false,
        sortPlugins = false,
        unusedKapt = false,
        anvilFactoryGeneration = false,
        disableAndroidResources = false,
        disableViewBinding = false,
        unusedKotlinAndroidExtensions = false,
        depths = true
      )
    ).apply {
      reports.depths.outputPath = File(workingDir, reports.depths.outputPath).path
      reports.graphs.outputDir = workingDir.resolve("graphs").absolutePath
    }
  }

  val RunnerTestEnvironment.depthsOutput
    get() = File(settings.reports.depths.outputPath)

  @Test
  fun `main source set depths should be reported`() = test {

    val lib1 = kotlinProject(":lib1")

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    kotlinProject(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    run(
      autoCorrect = false,
      findingFactory = findingFactory
    )

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
        -- ModuleCheck main source set depth results --
            depth    modules
            0        [:lib1]
            1        [:lib2]
            2        [:app]

        ModuleCheck found 0 issues
    """
  }

  @Test
  fun `reported depths should be from after fixes are applied`() = test {

    settings.checks.unusedDependency = true
    settings.checks.depths = true

    val lib1 = kotlinProject(":lib1")

    kotlinProject(":lib2") {
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
    }

    run(
      findingFactory = FindingFactoryImpl(rules)
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

        ModuleCheck found 1 issue
    """
  }

  @Test
  fun `test source set depths should not be reported`() = test {

    val lib1 = kotlinProject(":lib1")

    val lib2 = kotlinProject(":lib2") {
      addDependency(ConfigurationName.implementation, lib1)
    }

    kotlinProject(":app") {
      addDependency(ConfigurationName.implementation, lib1)
      addDependency(ConfigurationName.implementation, lib2)
    }

    lib1.addDependency(ConfigurationName("testImplementation"), lib2)

    run(
      autoCorrect = false,
      findingFactory = findingFactory
    )

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
        -- ModuleCheck main source set depth results --
            depth    modules
            0        [:lib1]
            1        [:lib2]
            2        [:app]

        ModuleCheck found 0 issues
    """
  }

  @Test
  fun `debug source set depth should not be reported even if it's longer`() = test {

    val lib1 = kotlinProject(":lib1")
    val debug1 = kotlinProject(":debug1")

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
      addDependency(ConfigurationName("debugImplementation"), debug2)
    }

    run(
      autoCorrect = false,
      findingFactory = findingFactory
    )

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
        -- ModuleCheck main source set depth results --
            depth    modules
            0        [:debug1, :lib1]
            1        [:lib2]
            2        [:app, :debug2]

        ModuleCheck found 0 issues
    """
  }

  @TestFactory
  fun `all outputs around depth findings should behave according to their own settings`() =
    flags { depthsConsole, depthsReport, graphsReport ->

      settings.checks.depths = depthsConsole
      settings.reports.depths.enabled = depthsReport
      settings.reports.graphs.enabled = graphsReport

      if (graphsReport) {
        settings.reports.graphs.outputDir = workingDir.resolve("graphs").absolutePath
      }

      val lib1 = kotlinProject(":lib1")

      val lib2 = kotlinProject(":lib2") {
        addDependency(ConfigurationName.implementation, lib1)
      }

      kotlinProject(":app") {
        addDependency(ConfigurationName.implementation, lib1)
        addDependency(ConfigurationName.implementation, lib2)
      }

      run().isSuccess shouldBe true

      val consoleOutput = logger.collectReport()
        .joinToString()
        .clean()
        .remove("\u200B")

      if (depthsConsole || depthsReport || graphsReport) {
        consoleOutput shouldBe """
            -- ModuleCheck main source set depth results --
                depth    modules
                0        [:lib1]
                1        [:lib2]
                2        [:app]

            ModuleCheck found 0 issues
            """
      } else {
        consoleOutput shouldBe """
            ModuleCheck found 0 issues
            """
      }

      if (depthsReport) {
        depthsOutput shouldHaveText """
            -- ModuleCheck Depth results --

            :app
                source set      depth    most expensive dependencies
                main            2        [:lib2]

            :lib2
                source set      depth    most expensive dependencies
                main            1        [:lib1]

            """
      } else {
        depthsOutput.shouldNotExist()
      }

      if (graphsReport) {
        workingDir.resolve("graphs", "app", "main.dot") shouldHaveText """
            strict digraph {
              edge ["dir"="forward"]
              graph ["ratio"="0.5625","rankdir"="TB","label"=<<b>:app -- main</b>>,"labelloc"="t"]
              node ["style"="rounded,filled","shape"="box"]
              {
                edge ["dir"="none"]
                graph ["rank"="same"]
                ":lib1" ["fillcolor"="#F89820"]
              }
              {
                edge ["dir"="none"]
                graph ["rank"="same"]
                ":lib2" ["fillcolor"="#F89820"]
              }
              {
                edge ["dir"="none"]
                graph ["rank"="same"]
                ":app" ["fillcolor"="#F89820"]
              }
              ":app" -> ":lib2" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
              ":app" -> ":lib1" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
              ":lib2" -> ":lib1" ["arrowhead"="normal","style"="bold","color"="#FF6347"]
            }
            """
      } else {
        workingDir.resolve("graphs", "app", "main.dot").shouldNotExist()
      }
    }

  @SkipInStackTrace
  fun flags(
    block: suspend RunnerTestEnvironment.(Boolean, Boolean, Boolean) -> Unit
  ): Stream<out DynamicNode> {

    return listOf(true, false)
      .asContainers({ "depthsConsole: $it" }) { depthsConsole ->

        listOf(true, false)
          .asContainers({ "depthsReport: $it" }) { depthsReport ->
            listOf(true, false)
              .asTests({ "graphsReport: $it" }) { graphsReport ->
                block(depthsConsole, depthsReport, graphsReport)
              }
          }
      }
  }
}
