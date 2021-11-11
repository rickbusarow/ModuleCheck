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
import modulecheck.api.test.createSafely
import modulecheck.core.rule.DepthRule
import modulecheck.core.rule.SingleRuleFindingFactory
import modulecheck.parsing.ConfigurationName
import modulecheck.parsing.SourceSet
import modulecheck.parsing.SourceSetName
import org.junit.jupiter.api.Test
import java.io.File

internal class DepthReportTest : ProjectTest() {

  val baseSettings by resets {
    TestSettings().apply {
      reports.depths.outputPath = File(testProjectDir, reports.depths.outputPath).path
    }
  }
  val logger by resets { ReportingLogger() }
  val findingFactory by resets { SingleRuleFindingFactory(DepthRule()) }

  val outputFile by resets { File(baseSettings.reports.depths.outputPath) }

  @Test
  fun `depth report should not be created if disabled in settings`() {

    baseSettings.reports.depths.enabled = false

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {}

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

    baseSettings.reports.depths.enabled = true

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {}

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

    baseSettings.reports.depths.enabled = true

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
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

    baseSettings.reports.depths.enabled = true

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
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

    baseSettings.reports.depths.enabled = true

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {
      addSourceSet(SourceSetName("test"))
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
  fun `debug source set depth should be reported`() {

    baseSettings.reports.depths.enabled = true

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = findingFactory,
      logger = logger
    )

    val lib1 = project(":lib1") {}
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
