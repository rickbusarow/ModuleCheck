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

package modulecheck.utils

import io.kotest.matchers.shouldBe
import modulecheck.utils.CaseMatcher.CamelSnakeCaseMatcher
import modulecheck.utils.CaseMatcher.KebabCaseMatcher
import modulecheck.utils.CaseMatcher.LowerCamelCaseMatcher
import modulecheck.utils.CaseMatcher.LowerFlatCaseMatcher
import modulecheck.utils.CaseMatcher.ScreamingKebabCaseMatcher
import modulecheck.utils.CaseMatcher.ScreamingSnakeCaseMatcher
import modulecheck.utils.CaseMatcher.SnakeCaseMatcher
import modulecheck.utils.CaseMatcher.TrainCaseMatcher
import modulecheck.utils.CaseMatcher.UpperCamelCaseMatcher
import modulecheck.utils.CaseMatcher.UpperFlatCaseMatcher
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

internal class CaseMatcherTest {

  @TestFactory
  fun `lower flat case matches`() = listOf(
    "twoabcwords",
    "twowords",
    "twowords456",
    "twowordsabc"
  ).dynamic("should match") {

    LowerFlatCaseMatcher().matches(it) shouldBe true
  }

  @TestFactory
  fun `lower flat case non-matches`() = listOf(
    "    ",
    "",
    "ONE_WORD_",
    "ONE_WORD_123",
    "TWO-ABC-WORDS",
    "TWO-WORDS",
    "TWO-WORDS-ABC",
    "TWO-WORDS456",
    "TWOABCWORDS",
    "TWOWORDS",
    "TWOWORDS456",
    "TWOWORDSABC",
    "TWO_ABC_WORDS",
    "TWO_WORDS",
    "TWO_WORDS456",
    "TWO_WORDSABC",
    "TWO_WORDS_ABC",
    "Two-ABC-Words",
    "Two-Words",
    "Two-Words-ABC",
    "Two-Words456",
    "TwoABCWords",
    "Two_ABC_Words",
    "Two_Words",
    "Two_Words456",
    "Two_Words_ABC",
    "two-abc-words",
    "two-words",
    "two-words-abc",
    "two-words456",
    "twoABCWords",
    "twoWords",
    "twoWords456",
    "twoWordsABC",
    "two_abc_words",
    "two_words",
    "two_words456",
    "two_words_abc"
  ).dynamic("should not match") {

    LowerFlatCaseMatcher().matches(it) shouldBe false
  }

  @TestFactory
  fun `upper flat case matches`() = listOf(
    "TWOABCWORDS",
    "TWOWORDS",
    "TWOWORDS456",
    "TWOWORDSABC"
  ).dynamic("should match") {

    UpperFlatCaseMatcher().matches(it) shouldBe true
  }

  @TestFactory
  fun `upper flat case non-matches`() = listOf(
    "    ",
    "",
    "ONE_WORD_",
    "ONE_WORD_123",
    "TWO-ABC-WORDS",
    "TWO-WORDS",
    "TWO-WORDS-ABC",
    "TWO-WORDS456",
    "TWO_ABC_WORDS",
    "TWO_WORDS",
    "TWO_WORDS456",
    "TWO_WORDSABC",
    "TWO_WORDS_ABC",
    "Two-ABC-Words",
    "Two-Words",
    "Two-Words-ABC",
    "Two-Words456",
    "TwoABCWords",
    "Two_ABC_Words",
    "Two_Words",
    "Two_Words456",
    "Two_Words_ABC",
    "two-abc-words",
    "two-words",
    "two-words-abc",
    "two-words456",
    "twoABCWords",
    "twoWords",
    "twoWords456",
    "twoWordsABC",
    "two_abc_words",
    "two_words",
    "two_words456",
    "two_words_abc",
    "twoabcwords",
    "twowords",
    "twowords456",
    "twowordsabc"
  ).dynamic("should not match") {

    UpperFlatCaseMatcher().matches(it) shouldBe false
  }

  @TestFactory
  fun `lower camel case matches`() = listOf(
    "twoABCWords",
    "twoWords",
    "twoWords456",
    "twoWordsABC",
    "twoabcwords",
    "twowords",
    "twowords456",
    "twowordsabc"
  ).dynamic("should match") {

    LowerCamelCaseMatcher().matches(it) shouldBe true
  }

  @TestFactory
  fun `lower camel case non-matches`() = listOf(
    "    ",
    "",
    "ONE_WORD_",
    "ONE_WORD_123",
    "TWO-ABC-WORDS",
    "TWO-WORDS",
    "TWO-WORDS-ABC",
    "TWO-WORDS456",
    "TWOABCWORDS",
    "TWOWORDS",
    "TWOWORDS456",
    "TWOWORDSABC",
    "TWO_ABC_WORDS",
    "TWO_WORDS",
    "TWO_WORDS456",
    "TWO_WORDSABC",
    "TWO_WORDS_ABC",
    "Two-ABC-Words",
    "Two-Words",
    "Two-Words-ABC",
    "Two-Words456",
    "TwoABCWords",
    "Two_ABC_Words",
    "Two_Words",
    "Two_Words456",
    "Two_Words_ABC",
    "two-abc-words",
    "two-words",
    "two-words-abc",
    "two-words456",
    "two_abc_words",
    "two_words",
    "two_words456",
    "two_words_abc"
  ).dynamic("should not match") {

    LowerCamelCaseMatcher().matches(it) shouldBe false
  }

  @TestFactory
  fun `upper camel case matches`() = listOf(
    "TwoABCWords"
  ).dynamic("should match") {

    UpperCamelCaseMatcher().matches(it) shouldBe true
  }

  @TestFactory
  fun `upper camel case non-matches`() = listOf(
    "    ",
    "",
    "ONE_WORD_",
    "ONE_WORD_123",
    "TWO-ABC-WORDS",
    "TWO-WORDS",
    "TWO-WORDS-ABC",
    "TWO-WORDS456",
    "TWOABCWORDS",
    "TWOWORDS",
    "TWOWORDS456",
    "TWOWORDSABC",
    "TWO_ABC_WORDS",
    "TWO_WORDS",
    "TWO_WORDS456",
    "TWO_WORDS_ABC",
    "Two-ABC-Words",
    "Two-Words",
    "Two-Words-ABC",
    "Two-Words456",
    "Two_ABC_Words",
    "Two_Words",
    "Two_Words456",
    "Two_Words_ABC",
    "two-abc-words",
    "two-words",
    "two-words-abc",
    "two-words456",
    "twoABCWords",
    "twoWords",
    "twoWords456",
    "twoWordsABC",
    "two_abc_words",
    "two_words",
    "two_words456",
    "two_words_abc",
    "twoabcwords",
    "twowords",
    "twowords456",
    "twowordsabc"
  ).dynamic("should not match") {

    UpperCamelCaseMatcher().matches(it) shouldBe false
  }

  @TestFactory
  fun `snake case matches`() = listOf(
    "two_abc_words",
    "two_words",
    "two_words456",
    "two_words_abc",
    "twoabcwords",
    "twowords",
    "twowords456",
    "twowordsabc"
  ).dynamic("should match") {

    SnakeCaseMatcher().matches(it) shouldBe true
  }

  @TestFactory
  fun `snake case non-matches`() = listOf(
    "    ",
    "",
    "ONE_WORD_",
    "ONE_WORD_123",
    "TWO-ABC-WORDS",
    "TWO-WORDS",
    "TWO-WORDS-ABC",
    "TWO-WORDS456",
    "TWOABCWORDS",
    "TWOWORDS",
    "TWOWORDS456",
    "TWOWORDSABC",
    "TWO_ABC_WORDS",
    "TWO_WORDS",
    "TWO_WORDS456",
    "TWO_WORDSABC",
    "TWO_WORDS_ABC",
    "Two-ABC-Words",
    "Two-Words",
    "Two-Words-ABC",
    "Two-Words456",
    "TwoABCWords",
    "Two_ABC_Words",
    "Two_Words",
    "Two_Words456",
    "Two_Words_ABC",
    "two-abc-words",
    "two-words",
    "two-words-abc",
    "two-words456",
    "twoABCWords",
    "twoWords",
    "twoWords456",
    "twoWordsABC"
  ).dynamic("should not match") {

    SnakeCaseMatcher().matches(it) shouldBe false
  }

  @TestFactory
  fun `screaming snake case matches`() = listOf(
    "ONE_WORD_123",
    "TWOABCWORDS",
    "TWOWORDS",
    "TWOWORDS456",
    "TWOWORDSABC",
    "TWO_ABC_WORDS",
    "TWO_WORDS",
    "TWO_WORDS456",
    "TWO_WORDS_ABC"
  ).dynamic("should match") {

    ScreamingSnakeCaseMatcher().matches(it) shouldBe true
  }

  @TestFactory
  fun `screaming snake case non-matches`() = listOf(
    "    ",
    "",
    "ONE_WORD_",
    "TWO-ABC-WORDS",
    "TWO-WORDS",
    "TWO-WORDS-ABC",
    "TWO-WORDS456",
    "Two-ABC-Words",
    "Two-Words",
    "Two-Words-ABC",
    "Two-Words456",
    "TwoABCWords",
    "Two_ABC_Words",
    "Two_Words",
    "Two_Words456",
    "Two_Words_ABC",
    "two-abc-words",
    "two-words",
    "two-words-abc",
    "two-words456",
    "twoABCWords",
    "twoWords",
    "twoWords456",
    "twoWordsABC",
    "two_abc_words",
    "two_words",
    "two_words456",
    "two_words_abc",
    "twoabcwords",
    "twowords",
    "twowords456",
    "twowordsabc"
  ).dynamic("should not match") {

    ScreamingSnakeCaseMatcher().matches(it) shouldBe false
  }

  @TestFactory
  fun `camel snake case matches`() = listOf(
    "Two_ABC_Words",
    "Two_Words",
    "Two_Words456",
    "Two_Words_ABC"
  ).dynamic("should match") {

    CamelSnakeCaseMatcher().matches(it) shouldBe true
  }

  @TestFactory
  fun `camel snake case non-matches`() = listOf(
    "    ",
    "",
    "ONE_WORD_",
    "ONE_WORD_123",
    "TWO-ABC-WORDS",
    "TWO-WORDS",
    "TWO-WORDS-ABC",
    "TWO-WORDS456",
    "TWOABCWORDS",
    "TWOWORDS",
    "TWOWORDS456",
    "TWOWORDSABC",
    "TWO_ABC_WORDS",
    "TWO_WORDS",
    "TWO_WORDS456",
    "TWO_WORDSABC",
    "TWO_WORDS_ABC",
    "Two-ABC-Words",
    "Two-Words",
    "Two-Words-ABC",
    "Two-Words456",
    "TwoABCWords",
    "two-abc-words",
    "two-words",
    "two-words-abc",
    "two-words456",
    "twoABCWords",
    "twoWords",
    "twoWords456",
    "twoWordsABC",
    "two_abc_words",
    "two_words",
    "two_words456",
    "two_words_abc",
    "twoabcwords",
    "twowords",
    "twowords456",
    "twowordsabc"
  ).dynamic("should not match") {

    CamelSnakeCaseMatcher().matches(it) shouldBe false
  }

  @TestFactory
  fun `kebab case matches`() = listOf(
    "two-abc-words",
    "two-words",
    "two-words-abc",
    "two-words456",
    "twoabcwords",
    "twowords",
    "twowords456",
    "twowordsabc"
  ).dynamic("should match") {

    KebabCaseMatcher().matches(it) shouldBe true
  }

  @TestFactory
  fun `kebab case non-matches`() = listOf(
    "    ",
    "",
    "ONE_WORD_",
    "ONE_WORD_123",
    "TWO-ABC-WORDS",
    "TWO-WORDS",
    "TWO-WORDS-ABC",
    "TWO-WORDS456",
    "TWOABCWORDS",
    "TWOWORDS",
    "TWOWORDS456",
    "TWOWORDSABC",
    "TWO_ABC_WORDS",
    "TWO_WORDS",
    "TWO_WORDS456",
    "TWO_WORDSABC",
    "TWO_WORDS_ABC",
    "Two-ABC-Words",
    "Two-Words",
    "Two-Words-ABC",
    "Two-Words456",
    "TwoABCWords",
    "Two_ABC_Words",
    "Two_Words",
    "Two_Words456",
    "Two_Words_ABC",
    "twoABCWords",
    "twoWords",
    "twoWords456",
    "twoWordsABC",
    "two_abc_words",
    "two_words",
    "two_words456",
    "two_words_abc"
  ).dynamic("should not match") {

    KebabCaseMatcher().matches(it) shouldBe false
  }

  @TestFactory
  fun `screaming kebab case matches`() = listOf(
    "TWO-ABC-WORDS",
    "TWO-WORDS",
    "TWO-WORDS-ABC",
    "TWO-WORDS456",
    "TWOABCWORDS",
    "TWOWORDS",
    "TWOWORDS456",
    "TWOWORDSABC"

  ).dynamic("should match") {

    ScreamingKebabCaseMatcher().matches(it) shouldBe true
  }

  @TestFactory
  fun `screaming kebab case non-matches`() = listOf(

    "    ",
    "",
    "ONE_WORD_",
    "ONE_WORD_123",
    "TWO_ABC_WORDS",
    "TWO_WORDS",
    "TWO_WORDS456",
    "TWO_WORDSABC",
    "TWO_WORDS_ABC",
    "Two-ABC-Words",
    "Two-Words",
    "Two-Words-ABC",
    "Two-Words456",
    "TwoABCWords",
    "Two_ABC_Words",
    "Two_Words",
    "Two_Words456",
    "Two_Words_ABC",
    "two-abc-words",
    "two-words",
    "two-words-abc",
    "two-words456",
    "twoABCWords",
    "twoWords",
    "twoWords456",
    "twoWordsABC",
    "two_abc_words",
    "two_words",
    "two_words456",
    "two_words_abc",
    "twoabcwords",
    "twowords",
    "twowords456",
    "twowordsabc"
  ).dynamic("should not match") {

    ScreamingKebabCaseMatcher().matches(it) shouldBe false
  }

  @TestFactory
  fun `train case matches`() = listOf(
    "ONE_WORD_",
    "ONE_WORD_123"
  ).dynamic("should match") {

    TrainCaseMatcher().matches(it) shouldBe false
  }

  @TestFactory
  fun `train case non-matches`() = listOf(
    "    ",
    "",
    "ONE_WORD_",
    "ONE_WORD_123",
    "TWO-ABC-WORDS",
    "TWO-WORDS",
    "TWO-WORDS-ABC",
    "TWO-WORDS456",
    "TWOABCWORDS",
    "TWOWORDS",
    "TWOWORDS456",
    "TWOWORDSABC",
    "TWO_ABC_WORDS",
    "TWO_WORDS",
    "TWO_WORDS456",
    "TWO_WORDSABC",
    "TWO_WORDS_ABC",
    "TwoABCWords",
    "Two_ABC_Words",
    "Two_Words",
    "Two_Words456",
    "Two_Words_ABC",
    "two-abc-words",
    "two-words",
    "two-words-abc",
    "two-words456",
    "twoABCWords",
    "twoWords",
    "twoWords456",
    "twoWordsABC",
    "two_abc_words",
    "two_words",
    "two_words456",
    "two_words_abc",
    "twoabcwords",
    "twowords",
    "twowords456",
    "twowordsabc"
  ).dynamic("should not match") {

    TrainCaseMatcher().matches(it) shouldBe false
  }

  fun List<String>.dynamic(extraName: String = "", test: (String) -> Unit): List<DynamicTest> {
    return map { subject ->

      val displayName = if (extraName.isNotBlank()) {
        "'$subject' -- $extraName"
      } else {
        subject
      }

      DynamicTest.dynamicTest(displayName) {
        test.invoke(subject)
      }
    }
  }
}
