/*
 * Copyright (C) 2021-2024 Rick Busarow
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

package modulecheck.parsing.psi

import modulecheck.parsing.gradle.dsl.PluginDeclaration
import modulecheck.parsing.kotlin.compiler.NoContextPsiFileFactory
import modulecheck.reporting.logging.PrintLogger
import modulecheck.testing.BaseTest
import org.junit.jupiter.api.Test

internal class KotlinPluginsBlockParserTest : BaseTest {

  @Test
  fun `external declaration`() = test {
    val block = parse(
      """
       plugins {
         kotlin("jvm") // trailing comment
         // comment
         javaLibrary
         id("io.gitlab.arturbosch.detekt") version "1.15.0"
       }
      """.trimIndent()
    )

    block.settings shouldBe listOf(
      PluginDeclaration(
        declarationText = """kotlin("jvm")""",
        statementWithSurroundingText = """  kotlin("jvm") // trailing comment""",
        suppressed = emptyList()
      ),
      PluginDeclaration(
        declarationText = """javaLibrary""",
        statementWithSurroundingText = """  // comment
          |  javaLibrary
        """.trimMargin(),
        suppressed = emptyList()
      ),
      PluginDeclaration(
        declarationText = """id("io.gitlab.arturbosch.detekt") version "1.15.0"""",
        statementWithSurroundingText = """  id("io.gitlab.arturbosch.detekt") version "1.15.0"""",
        suppressed = emptyList()
      )
    )
  }

  @Test
  fun `suppressed kotlin function`() = test {
    val block = parse(
      """
       plugins {
         @Suppress("unused-plugin")
         kotlin("jvm")
       }
      """.trimIndent()
    )

    block.settings shouldBe listOf(
      PluginDeclaration(
        declarationText = """kotlin("jvm")""",
        statementWithSurroundingText = """
          |  @Suppress("unused-plugin")
          |  kotlin("jvm")
        """.trimMargin(),
        suppressed = listOf("unused-plugin")
      )
    )
  }

  @Test
  fun `suppressed back-ticked ID`() = test {
    val block = parse(
      """
       plugins {
         @Suppress("unused-plugin")
         `kotlin-jvm`
       }
      """.trimIndent()
    )

    block.settings shouldBe listOf(
      PluginDeclaration(
        declarationText = """`kotlin-jvm`""",
        statementWithSurroundingText = """
          |  @Suppress("unused-plugin")
          |  `kotlin-jvm`
        """.trimMargin(),
        suppressed = listOf("unused-plugin")
      )
    )
  }

  @Test
  fun `suppression which doesn't match finding name regex should be ignored`() = test {
    val block = parse(
      """
       @Suppress("DSL_SCOPE_VIOLATION")
       plugins {
         id("com.squareup.anvil")
       }
       """
    )

    block.allSuppressions.values.flatten() shouldBe emptyList()
  }

  @Test
  fun `suppressed id function`() = test {
    val block = parse(
      """
       plugins {
         @Suppress("unused-plugin")
         id("com.squareup.anvil")
       }
      """.trimIndent()
    )

    block.settings shouldBe listOf(
      PluginDeclaration(
        declarationText = """id("com.squareup.anvil")""",
        statementWithSurroundingText = """
          |  @Suppress("unused-plugin")
          |  id("com.squareup.anvil")
        """.trimMargin(),
        suppressed = listOf("unused-plugin")
      )
    )
  }

  @Test
  fun `suppressed with old finding name`() = test {
    val block = parse(
      """
       plugins {
         @Suppress("UnusedKaptProcessor")
         id("com.squareup.anvil")
       }
      """.trimIndent()
    )

    block.settings shouldBe listOf(
      PluginDeclaration(
        declarationText = """id("com.squareup.anvil")""",
        statementWithSurroundingText = """
          |  @Suppress("UnusedKaptProcessor")
          |  id("com.squareup.anvil")
        """.trimMargin(),
        suppressed = listOf("unused-kapt-processor")
      )
    )
  }

  suspend fun parse(string: String): KotlinPluginsBlock {
    val file = NoContextPsiFileFactory()
      .createKotlin("build.gradle.kts", string.trimIndent())

    return KotlinPluginsBlockParser(PrintLogger()).parse(file)!!
  }
}
