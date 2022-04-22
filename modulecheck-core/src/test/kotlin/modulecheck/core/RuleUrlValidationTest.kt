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

import io.kotest.assertions.asClue
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.file.shouldNotBeEmpty
import modulecheck.api.finding.Finding
import modulecheck.api.rule.ModuleCheckRule
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.api.test.TestSettings
import modulecheck.core.rule.DocumentedRule.Companion.RULES_BASE_URL
import modulecheck.core.rule.ModuleCheckRuleFactory
import modulecheck.testing.BaseTest
import modulecheck.testing.getPrivateFieldByName
import modulecheck.utils.child
import modulecheck.utils.remove
import modulecheck.utils.requireNotNull
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.absolute

class RuleUrlValidationTest : BaseTest() {

  @Test
  fun `url`() {

    val websiteDir = Path(".").absolute()
      .parent
      .parent
      .child("website")

    """
      The path must point to the project website: $websiteDir

      If this doesn't point to the website, something has moved.
    """.trimIndent().asClue {
      websiteDir.shouldExist()
      websiteDir.shouldNotBeEmpty()
    }

    val sidebarsFile = websiteDir.child("sidebars.js")

    "The path must point to the project website's `sidebars.js` file: $sidebarsFile ".asClue {
      sidebarsFile.shouldExist()
    }

    val sidebarsRules = sidebarsFile.readText().lines()
      .asSequence()
      .map { it.trim() }
      // strip out comments
      .filterNot { it.startsWith("//") || it.startsWith("/*") || it.startsWith("*") }
      .joinToString("\n")
      .let { text ->
        """"rules/([^"]+)"""".toRegex().findAll(text)
          .map { it.groupValues[1] }
      }
      .toSet()

    val rulesDocsDir = websiteDir.child("docs", "rules")

    // Find all markdown docs within the /website/docs/rules directory, and their IDs as used by the
    // sidebar.
    // The ID looks like a relative path, but it's not.  It's the relative path in terms of the
    // *directory*, but it doesn't use the file name for the last element -- it uses the "id" in the
    // file's header.
    // Given a file `/website/docs/rules/compiler/ksp/foo.md`  with an id of `id: bar`,
    // the full sidebar id is `rules/compiler/ksp/bar`.
    val rulesDocsSidebarsIds = rulesDocsDir.walkTopDown()
      .filter { it.isFile }
      .filter { it.extension == "md" || it.extension == "mdx" }
      .map { file ->
        val relativeDir = file.parentFile.relativeTo(rulesDocsDir).path

        val header = "---([\\s\\S]*)(?=---)".toRegex().find(file.readText())
          .requireNotNull { "This file ($file) doesn't appear to have a header?" }
          .groupValues[1]
          .trim()

        val simpleId = header.lines()
          .firstNotNullOf { line ->
            """id\s?:\s?(\S+).*""".toRegex()
              .find(line)
              .requireNotNull { "Could not find an ID in the header of file ($file)" }
              .groupValues[1]
          }

        when {
          relativeDir.isEmpty() -> simpleId
          else -> "$relativeDir/$simpleId"
        }
      }
      .sorted()
      .toSet()

    val ruleFactories: List<(ModuleCheckSettings) -> ModuleCheckRule<out Finding>> =
      ModuleCheckRuleFactory().getPrivateFieldByName("rules")

    ruleFactories
      .asSequence()
      .map { it(TestSettings()) }
      .map { rule ->
        rule.id to rule.documentationUrl.remove(RULES_BASE_URL)
      }
      .sortedBy { it.second }
      .forAll { (_, urlString) ->

        "rule should be declared in the sidebar".asClue {
          urlString shouldBeIn sidebarsRules
        }
        "rule should have a file in the rule docs with a corresponding ID".asClue {
          urlString shouldBeIn rulesDocsSidebarsIds
        }
      }
  }
}
