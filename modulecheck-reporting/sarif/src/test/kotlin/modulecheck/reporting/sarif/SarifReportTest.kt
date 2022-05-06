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

package modulecheck.reporting.sarif

import io.kotest.assertions.asClue
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.runBlocking
import modulecheck.config.ModuleCheckSettings
import modulecheck.config.fake.TestSettings
import modulecheck.core.UnusedDependencyFinding
import modulecheck.core.rule.ModuleCheckRuleFactory
import modulecheck.finding.Finding
import modulecheck.finding.FindingName
import modulecheck.parsing.gradle.ConfigurationName
import modulecheck.project.ConfiguredProjectDependency
import modulecheck.rule.ModuleCheckRule
import modulecheck.runtime.test.RunnerTest
import modulecheck.testing.getPrivateFieldByName
import modulecheck.utils.suffixIfNot
import org.junit.jupiter.api.Test
import java.io.File

@Suppress("BlockingMethodInNonBlockingContext")
class SarifReportTest : RunnerTest() {

  @Test
  fun `report with unused dependency`() = runBlocking {

    val rules = ModuleCheckRuleFactory()
      .getPrivateFieldByName<ModuleCheckRuleFactory,
        List<(ModuleCheckSettings) -> ModuleCheckRule<out Finding>>>("rules")
      .map { it(TestSettings()) }

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
      oldDependency = ConfiguredProjectDependency(ConfigurationName.api, p1, false),
      dependencyIdentifier = p1.path.value,
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
