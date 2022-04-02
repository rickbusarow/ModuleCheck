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

package modulecheck.parsing.groovy.antlr

import modulecheck.parsing.gradle.PluginDeclaration
import modulecheck.testing.BaseTest
import modulecheck.testing.createSafely
import modulecheck.testing.requireNotNullOrFail
import modulecheck.utils.child
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class GroovyPluginsBlockParserTest : BaseTest() {

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
        declarationText = """id 'org.jetbrains.kotlin.jvm'""",
        statementWithSurroundingText = """  id 'org.jetbrains.kotlin.jvm' // trailing comment"""
      ),
      PluginDeclaration(
        declarationText = """id 'com.squareup.anvil' version '2.34.0'""",
        statementWithSurroundingText = """  // comment
          |  id 'com.squareup.anvil' version '2.34.0'
        """.trimMargin()
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
        declarationText = """id 'io.gitlab.arturbosch.detekt' version '1.15.0'""",
        statementWithSurroundingText = """  id 'io.gitlab.arturbosch.detekt' version '1.15.0'"""
      ),
      PluginDeclaration(
        declarationText = """javaLibrary""",
        statementWithSurroundingText = """  javaLibrary"""
      ),
      PluginDeclaration(
        declarationText = """id 'org.jetbrains.kotlin.jvm'""",
        statementWithSurroundingText = """  id 'org.jetbrains.kotlin.jvm'"""
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
      .let { file -> GroovyPluginsBlockParser().parse(file) }
      .requireNotNullOrFail()
      .assertions()
  }
}
