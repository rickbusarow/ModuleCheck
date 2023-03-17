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

package modulecheck.builds.ktlint.rules

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import com.pinterest.ktlint.test.format as ktlintTestFormat

class KDocWrappingRuleTest {

  val rules = setOf(
    RuleProvider { KDocWrappingRule() }
  )

  @Test
  fun `threshold wrapping`() {

    rules.format(
      """
      /**
       * @property extercitatrekvsuion nostrud exerc mco laboris nisteghi ut aliquip ex ea
       *     desegrunt fugiat nulla pariatur. Excepteur sint occaecat cupidatat
       */
      data class Subject(
        val name: String,
        val age: Int
      )
      """.trimIndent()
    ) shouldBe """
      /**
       * @property extercitatrekvsuion nostrud exerc
       *   mco laboris nisteghi ut aliquip ex ea
       *   desegrunt fugiat nulla pariatur. Excepteur
       *   sint occaecat cupidatat
       */
      data class Subject(
        val name: String,
        val age: Int
      )
    """.trimIndent()
  }

  @Test
  fun `@see tags are not flattened`() {

    rules.format(
      """
      /**
       * First line
       *   second line
       *
       * @see SomeClass someClass     description
       * @see Object object     description
       * @since 0.10.0
       */
      data class Subject(
        val name: String,
        val age: Int
      )
      """.trimIndent()
    ) shouldBe """
      /**
       * First line second line
       *
       * @see SomeClass someClass description
       * @see Object object description
       * @since 0.10.0
       */
      data class Subject(
        val name: String,
        val age: Int
      )
    """.trimIndent()
  }

  @Test
  fun `text with a link and no extra spaces is left alone`() {

    rules.lint(
      """
      /**
       * comment with [Subject]
       */
      data class Subject(
        val name: String,
        val age: Int
      )
      """.trimIndent()
    ) shouldBe listOf()
  }

  @Test
  fun `an code fenced block with language in the default section is treated as a code block`() {

    rules.format(
      """
      /**
       * a comment
       *
       * ```kotlin
       * fun foo() = Unit
       * val result = foo()
       * ```
       *
       * @property name some name property
       * @property age some age property
       */
      data class Subject(
        val name: String,
        val age: Int
      )
      """.trimIndent()
    ) shouldBe """
      /**
       * a comment
       *
       * ```kotlin
       * fun foo() = Unit
       * val result = foo()
       * ```
       *
       * @property name some name property
       * @property age some age property
       */
      data class Subject(
        val name: String,
        val age: Int
      )
    """.trimIndent()
  }

  @Test
  fun `an code fenced block without language in the default section is treated as a code block`() {

    rules.format(
      """
      /**
       * a comment
       *
       * ```
       * fun foo() = Unit
       * val result = foo()
       * ```
       *
       * @property name some name property
       * @property age some age property
       */
      data class Subject(
        val name: String,
        val age: Int
      )
      """.trimIndent()
    ) shouldBe """
      /**
       * a comment
       *
       * ```
       * fun foo() = Unit
       * val result = foo()
       * ```
       *
       * @property name some name property
       * @property age some age property
       */
      data class Subject(
        val name: String,
        val age: Int
      )
    """.trimIndent()
  }

  @Test
  fun `an indented paragraph in the default section is treated as a code block`() {

    rules.format(
      """
      /**
       * a comment
       *
       *     fun foo() = Unit
       *     val result = foo()
       *
       * @property name some name property
       * @property age some age property
       */
      data class Subject(
        val name: String,
        val age: Int
      )
      """.trimIndent()
    ) shouldBe """
      /**
       * a comment
       *
       *     fun foo() = Unit
       *     val result = foo()
       *
       * @property name some name property
       * @property age some age property
       */
      data class Subject(
        val name: String,
        val age: Int
      )
    """.trimIndent()
  }

  @Test
  fun `an indented tag comment is moved left`() {

    rules.format(
      """
      /**
       * a comment
       *
       * A new paragraph.
       *
       * @property name some name property
       *
       *                Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor
       *                incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis
       *                nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
       *                Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu
       *                fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in
       *                culpa qui officia deserunt mollit anim id est laborum.
       * @property age some age property
       */
      data class Subject(
        val name: String,
        val age: Int
      )
      """.trimIndent()
    ) shouldBe """
      /**
       * a comment
       *
       * A new paragraph.
       *
       * @property name some name property
       *
       *   Lorem ipsum dolor sit amet, consectetur
       *   adipiscing elit, sed do eiusmod tempor
       *   incididunt ut labore et dolore magna aliqua.
       *   Ut enim ad minim veniam, quis nostrud
       *   exercitation ullamco laboris nisi ut aliquip
       *   ex ea commodo consequat. Duis aute irure
       *   dolor in reprehenderit in voluptate velit
       *   esse cillum dolore eu fugiat nulla pariatur.
       *   Excepteur sint occaecat cupidatat non
       *   proident, sunt in culpa qui officia deserunt
       *   mollit anim id est laborum.
       * @property age some age property
       */
      data class Subject(
        val name: String,
        val age: Int
      )
    """.trimIndent()
  }

  @Test
  fun `a paragraph after a tag comment is indented by two spaces`() {

    rules.format(
      """
      /**
       * a comment
       *
       * A new paragraph.
       *
       * @property name some name property
       *
       * Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut
       * labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco
       * laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in
       * voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat
       * non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
       * @property age some age property
       */
      data class Subject(
        val name: String,
        val age: Int
      )
      """.trimIndent()
    ) shouldBe """
      /**
       * a comment
       *
       * A new paragraph.
       *
       * @property name some name property
       *
       *   Lorem ipsum dolor sit amet, consectetur
       *   adipiscing elit, sed do eiusmod tempor
       *   incididunt ut labore et dolore magna aliqua.
       *   Ut enim ad minim veniam, quis nostrud
       *   exercitation ullamco laboris nisi ut aliquip
       *   ex ea commodo consequat. Duis aute irure
       *   dolor in reprehenderit in voluptate velit
       *   esse cillum dolore eu fugiat nulla pariatur.
       *   Excepteur sint occaecat cupidatat non
       *   proident, sunt in culpa qui officia deserunt
       *   mollit anim id est laborum.
       * @property age some age property
       */
      data class Subject(
        val name: String,
        val age: Int
      )
    """.trimIndent()
  }

  @Test
  fun `a long single-line tag description is wrapped and indented`() {

    rules.format(
      """
      /**
       * a comment
       *
       * @property name Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
       * @property age some age property
       */
      data class Subject(
        val name: String,
        val age: Int
      )
      """.trimIndent()
    ) shouldBe """
      /**
       * a comment
       *
       * @property name Lorem ipsum dolor sit amet,
       *   consectetur adipiscing elit, sed do eiusmod
       *   tempor incididunt ut labore et dolore magna
       *   aliqua.
       * @property age some age property
       */
      data class Subject(
        val name: String,
        val age: Int
      )
    """.trimIndent()
  }

  @Test
  fun `a correctly formatted kdoc does not emit`() {

    rules.lint(
      """
      /**
       * a comment
       *
       * A new paragraph.
       *
       * @property name Lorem ipsum dolor sit amet,
       *   consectetur adipiscing elit, sed do eiusmod
       *   tempor incididunt ut labore et dolore magna
       *   aliqua.
       * @property age some age property
       */
      data class Subject(
        val name: String,
        val age: Int
      )
      """.trimIndent()
    ).shouldBeEmpty()
  }

  @Test
  fun `code blocks are not wrapped`() {

    rules.format(
      """
      /**
       * Given this
       * code:
       *
       * ```java
       * val seq = TODO()
       * ```
       * followed by:
       * ```
       * fun foo() = Unit
       * ```
       * do some things
       */
      data class Subject(
        val name: String,
        val age: Int
      )
      """.trimIndent()
    ) shouldBe """
      /**
       * Given this code:
       *
       * ```java
       * val seq = TODO()
       * ```
       *
       * followed by:
       *
       * ```
       * fun foo() = Unit
       * ```
       *
       * do some things
       */
      data class Subject(
        val name: String,
        val age: Int
      )
    """.trimIndent()
  }

  fun Set<RuleProvider>.format(
    @Language("kotlin")
    text: String,
    editorConfigOverride: EditorConfigOverride =
      EditorConfigOverride.from(MAX_LINE_LENGTH_PROPERTY to 50)
  ): String = ktlintTestFormat(
    text = text,
    filePath = null,
    editorConfigOverride = editorConfigOverride,
  )
    .first

  fun Set<RuleProvider>.lint(
    @Language("kotlin")
    text: String,
    editorConfigOverride: EditorConfigOverride =
      EditorConfigOverride.from(MAX_LINE_LENGTH_PROPERTY to 50)
  ): List<LintError> = lint(
    lintedFilePath = null,
    text = text,
    editorConfigOverride = editorConfigOverride
  )
}
