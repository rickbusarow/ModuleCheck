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

import modulecheck.config.fake.TestSettings
import modulecheck.core.anvil.CouldUseAnvilFinding
import modulecheck.parsing.gradle.ProjectPath.StringProjectPath
import modulecheck.rule.RuleName
import modulecheck.rule.finding.Finding
import modulecheck.runtime.test.RunnerTest
import modulecheck.utils.remove
import org.junit.jupiter.api.Test
import java.io.File

internal class TextReportingTest : RunnerTest() {

  override val settings by resets {
    TestSettings().apply {
      reports.text.outputPath = File(testProjectDir, reports.text.outputPath).path
    }
  }

  @Test
  fun `text report should not be created if disabled in settings`() {

    settings.reports.text.enabled = false

    val outputFile = File(settings.reports.text.outputPath)

    run(
      autoCorrect = false,
      findingFactory = findingFactory(
        listOf(
          CouldUseAnvilFinding(
            ruleName = RuleName("use-anvil-factory-generation"),
            dependentProject = kotlinProject(":lib1"),
            buildFile = testProjectDir
          )
        )
      ),
      findingResultFactory = { _, _, _ ->
        listOf(
          Finding.FindingResult(
            dependentPath = StringProjectPath(":dependentPath"),
            ruleName = RuleName("use-anvil-factory-generation"),
            sourceOrNull = "sourceOrNull",
            configurationName = "configurationName",
            dependencyIdentifier = "dependencyIdentifier",
            positionOrNull = Finding.Position(1, 2),
            buildFile = File("buildFile"),
            message = "message",
            fixed = true
          )
        )
      }
    ).isSuccess shouldBe true

    outputFile.exists() shouldBe false
  }

  @Test
  fun `text report should be created if enabled in settings`() {

    settings.reports.text.enabled = true

    val outputFile = File(settings.reports.text.outputPath)

    run(
      autoCorrect = false,
      findingFactory = findingFactory(
        listOf(
          CouldUseAnvilFinding(
            ruleName = RuleName("use-anvil-factory-generation"),
            dependentProject = kotlinProject(":lib1"),
            buildFile = testProjectDir
          )
        )
      ),
      findingResultFactory = { findings, _, _ ->
        findings.map {
          Finding.FindingResult(
            dependentPath = StringProjectPath(":dependentPath"),
            ruleName = RuleName("use-anvil-factory-generation"),
            sourceOrNull = "sourceOrNull",
            configurationName = "configurationName",
            dependencyIdentifier = "dependencyIdentifier",
            positionOrNull = Finding.Position(1, 2),
            buildFile = File("buildFile"),
            message = "message",
            fixed = true
          )
        }
      }
    ).isSuccess shouldBe true

    outputFile.readText()
      .clean()
      .remove("\u200B") shouldBe """
      :dependentPath
             configuration        dependency              name                            source          build file
          ✔  configurationName    dependencyIdentifier    use-anvil-factory-generation    sourceOrNull    buildFile: (1, 2):
    """
  }
}
