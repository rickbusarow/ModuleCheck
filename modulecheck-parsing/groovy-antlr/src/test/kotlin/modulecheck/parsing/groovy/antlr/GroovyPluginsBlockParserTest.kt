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

package modulecheck.parsing.groovy.antlr

import modulecheck.parsing.gradle.dsl.PluginDeclaration
import modulecheck.reporting.logging.PrintLogger
import modulecheck.testing.BaseTest
import modulecheck.testing.requireNotNullOrFail
import modulecheck.utils.child
import modulecheck.utils.createSafely
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class GroovyPluginsBlockParserTest : BaseTest() {

  val logger by resets { PrintLogger() }

  @Test
  fun `external declaration`() = parse(
    """
    plugins {
      id 'org.jetbrains.kotlin.jvm' // trailing comment
      // comment
      id 'com.squareup.anvil' version '2.34.0'
    }
    """.trimIndent()
  ) {

    settings shouldBe listOf(
      PluginDeclaration(
        statementWithSurroundingText = """  id 'org.jetbrains.kotlin.jvm' // trailing comment""",
        declarationText = """id 'org.jetbrains.kotlin.jvm'""",
        suppressed = emptyList()
      ),
      PluginDeclaration(
        statementWithSurroundingText = """  // comment
          |  id 'com.squareup.anvil' version '2.34.0'
        """.trimMargin(),
        declarationText = """id 'com.squareup.anvil' version '2.34.0'""",
        suppressed = emptyList()
      )
    )
  }

  @Test
  fun `single declarations should only be counted once`() = parse(
    """
    plugins {
      id 'io.gitlab.arturbosch.detekt' version '1.15.0'
      javaLibrary
      id 'org.jetbrains.kotlin.jvm'
    }
    """.trimIndent()
  ) {

    settings shouldBe listOf(
      PluginDeclaration(
        statementWithSurroundingText = """  id 'io.gitlab.arturbosch.detekt' version '1.15.0'""",
        declarationText = """id 'io.gitlab.arturbosch.detekt' version '1.15.0'""",
        suppressed = emptyList()
      ),
      PluginDeclaration(
        statementWithSurroundingText = """  javaLibrary""",
        declarationText = """javaLibrary""",
        suppressed = emptyList()
      ),
      PluginDeclaration(
        statementWithSurroundingText = """  id 'org.jetbrains.kotlin.jvm'""",
        declarationText = """id 'org.jetbrains.kotlin.jvm'""",
        suppressed = emptyList()
      )
    )
  }

  @Test
  fun `single suppress at declaration`() = parse(
    """
    plugins {
      //noinspection finding-1
      id 'io.gitlab.arturbosch.detekt' version '1.15.0'
      //noinspection finding-2
      javaLibrary
      //noinspection finding-3
      id 'org.jetbrains.kotlin.jvm'
    }
    """.trimIndent()
  ) {

    settings shouldBe listOf(
      PluginDeclaration(
        statementWithSurroundingText = "  //noinspection finding-1\n" +
          "  id 'io.gitlab.arturbosch.detekt' version '1.15.0'",
        declarationText = """id 'io.gitlab.arturbosch.detekt' version '1.15.0'""",
        suppressed = listOf("finding-1")
      ),
      PluginDeclaration(
        statementWithSurroundingText = "  //noinspection finding-2\n  javaLibrary",
        declarationText = """javaLibrary""",
        suppressed = listOf("finding-2")
      ),
      PluginDeclaration(
        statementWithSurroundingText = "  //noinspection finding-3\n  id 'org.jetbrains.kotlin.jvm'",
        declarationText = """id 'org.jetbrains.kotlin.jvm'""",
        suppressed = listOf("finding-3")
      )
    )
  }

  @Test
  fun `suppression which doesn't match finding name regex should be ignored`() = parse(
    """
       //noinspection DSL_SCOPE_VIOLATION
       plugins {
         id 'com.squareup.anvil'
       }
       """
  ) {

    allSuppressions.values.flatten() shouldBe emptyList()
  }

  @Test
  fun `suppress with old ID should be updated`() = parse(
    """
    plugins {
      //noinspection UnusedKaptProcessor
      id 'io.gitlab.arturbosch.detekt' version '1.15.0'
    }
    """.trimIndent()
  ) {

    settings shouldBe listOf(
      PluginDeclaration(
        statementWithSurroundingText = "  //noinspection UnusedKaptProcessor\n" +
          "  id 'io.gitlab.arturbosch.detekt' version '1.15.0'",
        declarationText = """id 'io.gitlab.arturbosch.detekt' version '1.15.0'""",
        suppressed = listOf("unused-kapt-processor")
      )
    )
  }

  inline fun parse(
    @Language("groovy")
    fileText: String,
    assertions: GroovyPluginsBlock.() -> Unit
  ) {
    testProjectDir.child("build.gradle")
      .createSafely(fileText.trimIndent())
      .let { file -> GroovyPluginsBlockParser(logger).parse(file) }
      .requireNotNullOrFail()
      .assertions()
  }
}
