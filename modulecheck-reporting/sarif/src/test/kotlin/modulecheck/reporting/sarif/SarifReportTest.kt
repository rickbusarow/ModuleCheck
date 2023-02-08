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

package modulecheck.reporting.sarif

import io.kotest.assertions.asClue
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.runBlocking
import modulecheck.config.ModuleCheckSettings
import modulecheck.config.fake.TestChecksSettings
import modulecheck.config.fake.TestSettings
import modulecheck.finding.FindingName
import modulecheck.finding.UnusedDependencyFinding
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.ProjectDependency.RuntimeProjectDependency
import modulecheck.runtime.test.RunnerTest
import modulecheck.utils.suffixIfNot
import org.junit.jupiter.api.Test
import java.io.File

@Suppress("BlockingMethodInNonBlockingContext")
class SarifReportTest : RunnerTest() {

  override val settings: ModuleCheckSettings by resets {
    TestSettings(
      checks = TestChecksSettings(
        redundantDependency = true,
        unusedDependency = true,
        overShotDependency = true,
        mustBeApi = true,
        inheritedDependency = true,
        sortDependencies = true,
        sortPlugins = true,
        unusedKapt = true,
        anvilFactoryGeneration = true,
        disableAndroidResources = true,
        disableViewBinding = true,
        unusedKotlinAndroidExtensions = true,
        depths = true
      )
    )
  }

  @Test
  fun `report with unused dependency`() = runBlocking {

    val factory = SarifReportFactory(
      websiteUrl = { "https://rbusarow.github.io/ModuleCheck" },
      moduleCheckVersion = { "0.12.1-SNAPSHOT" }
    ) { testProjectDir }

    val p1 = kotlinProject(":lib1")
    val p2 = kotlinProject(":lib2") {
      buildFile {
        """
        dependencies {
          api(project(":lib1"))
        }
        """
      }
    }

    val finding = UnusedDependencyFinding(
      findingName = FindingName("unused-dependency"),
      dependentProject = p2,
      oldDependency = RuntimeProjectDependency(ConfigurationName.api, p1.projectPath, false),
      dependencyIdentifier = p1.projectPath.value,
      configurationName = ConfigurationName.api
    ).toResult(true)

    val reportString = factory.create(
      findingResults = listOf(finding),
      rules = rules
    )

    val expected = this::class.java.classLoader
      .getResourceAsStream("sarif_report_with_unused_dependency_finding.sarif.json")!!
      .readAllBytes()
      .decodeToString()
      .also { fromFile ->

        """Couldn't find any `${"$"}TEST_DIR` tokens to replace.
          |Be sure to replace any hard-coded absolute paths with `${'$'}TEST_DIR` so that this test will pass on other machines.
        """.trimMargin()
          .asClue {
            fromFile shouldContain "\$TEST_DIR"
          }
      }
      .replace("\$TEST_DIR", testProjectDir.path)

    val normalizedReport = reportString
      .replace(
        p2.buildFile.relativePath()
          .removePrefix(File.separator)
          .replace(File.separator, "${Regex.escape(File.separator)}+")
          .toRegex(),
        p2.buildFile.relativePath()
          .removePrefix(File.separator)
          .alwaysUnixFileSeparators()
      )
      .replace(
        testProjectDir.absolutePath
          .suffixIfNot(File.separator)
          .replace(File.separator, "${Regex.escape(File.separator)}+")
          .toRegex(),
        testProjectDir.absolutePath
          .suffixIfNot(File.separator)
          .alwaysUnixFileSeparators()
      ).useRelativePaths()

    normalizedReport shouldBe expected.useRelativePaths()
  }
}
