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

package modulecheck.parsing.psi

import modulecheck.parsing.gradle.dsl.PluginDeclaration
import modulecheck.parsing.psi.internal.psiFileFactory
import modulecheck.testing.BaseTest
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.junit.jupiter.api.Test

internal class KotlinPluginsBlockParserTest : BaseTest() {

  @Test
  fun `external declaration`() {
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
        statementWithSurroundingText = """  kotlin("jvm") // trailing comment"""
      ),
      PluginDeclaration(
        declarationText = """javaLibrary""",
        statementWithSurroundingText = """  // comment
          |  javaLibrary
        """.trimMargin()
      ),
      PluginDeclaration(
        declarationText = """id("io.gitlab.arturbosch.detekt") version "1.15.0"""",
        statementWithSurroundingText = """  id("io.gitlab.arturbosch.detekt") version "1.15.0""""
      )
    )
  }

  fun parse(
    string: String
  ): KotlinPluginsBlock {
    val file = psiFileFactory
      .createFileFromText("build.gradle.kts", KotlinLanguage.INSTANCE, string)
      .cast<KtFile>()

    return KotlinPluginsBlockParser().parse(file)!!
  }
}
