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

package modulecheck.utils

import io.kotest.matchers.shouldBe
import modulecheck.utils.CaseMatcher.LowerCamelCaseMatcher
import modulecheck.utils.CaseMatcher.LowerFlatCaseMatcher
import modulecheck.utils.CaseMatcher.ScreamingSnakeCaseMatcher
import modulecheck.utils.CaseMatcher.SnakeCaseMatcher
import modulecheck.utils.CaseMatcher.UpperCamelCaseMatcher
import modulecheck.utils.CaseMatcher.UpperFlatCaseMatcher
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

internal class CaseMatcherTest2 {

  @TestFactory
  fun `lower flat case matches`() = listOf(
    "ONE_WORD_",
    "ONE_WORD_123",
    "twowords",
    "twowords456",
    "twowordsabc",
    "twoabcwords",
    "ONE_WORD_",
    "ONE_WORD_123",
    "ONE_WORD_",
    "ONE_WORD_123",
    "ONE_WORD_",
    "ONE_WORD_123"

  ).dynamic("should match") {

    LowerFlatCaseMatcher().matches(it) shouldBe true
  }

  @TestFactory
  fun `lower flat case non-matches`() = listOf(
    "    ",
    "",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
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
    "TWO_WORDS",
    "TWO_WORDS456",
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
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
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
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "TWO-ABC-WORDS",
    "TWO-WORDS",
    "TWO-WORDS-ABC",
    "TWO-WORDS456",
    "TWO_ABC_WORDS",
    "TWO_WORDS",
    "TWO_WORDS",
    "TWO_WORDS456",
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
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "twoABCWords",
    "twoWords",
    "twoWords456",
    "twoWordsABC",
    "twoabcwords",
    "twowords",
    "twowords456",
    "twowordsabc"
  ).dyanmic("should match") {

    LowerCamelCaseMatcher().matches(it) shouldBe true
  }

  @TestFactory
  fun `lower camel case non-matches`() = listOf(
    "    ",
    "",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
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
    "TWO_WORDS",
    "TWO_WORDS456",
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
  ).dyanmic("should not match") {

    LowerCamelCaseMatcher().matches(it) shouldBe false
  }

  @TestFactory
  fun `upper camel case matches`() = listOf(

    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "TWO_WORDS",
    "TWO_WORDS456",
    "TWO_WORDSABC",
    "TwoABCWords"
  ).dyanmic("should match") {

    UpperCamelCaseMatcher().matches(it) shouldBe true
  }

  @TestFactory
  fun `upper camel case non-matches`() = listOf(
    "    ",
    "",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
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
  ).dyanmic("should not match") {

    UpperCamelCaseMatcher().matches(it) shouldBe false
  }

  @TestFactory
  fun `snake case matches`() = listOf(

    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "two_abc_words",
    "two_words",
    "two_words456",
    "two_words_abc",
    "twoabcwords",
    "twowords",
    "twowords456",
    "twowordsabc"
  ).dyanmic("should match") {

    SnakeCaseMatcher().matches(it) shouldBe true
  }

  @TestFactory
  fun `snake case non-matches`() = listOf(
    "    ",
    "",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
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
    "TWO_WORDS",
    "TWO_WORDS456",
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
  ).dyanmic("should not match") {

    SnakeCaseMatcher().matches(it) shouldBe false
  }

  @TestFactory
  fun `screaming snake case matches`() = listOf(
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "TWOABCWORDS",
    "TWOWORDS",
    "TWOWORDS456",
    "TWOWORDSABC",
    "TWO_ABC_WORDS",
    "TWO_WORDS",
    "TWO_WORDS456",
    "TWO_WORDS_ABC"
  ).dyanmic("should match") {

    ScreamingSnakeCaseMatcher().matches(it) shouldBe true
  }

  @TestFactory
  fun `screaming snake case non-matches`() = listOf(
    "    ",
    "",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "ONE_WORD_123",
    "TWO-ABC-WORDS",
    "TWO-WORDS",
    "TWO-WORDS-ABC",
    "TWO-WORDS456",
    "TWO_WORDS",
    "TWO_WORDS456",
    "TWO_WORDSABC",
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
  ).dyanmic("should not match") {

    ScreamingSnakeCaseMatcher().matches(it) shouldBe false
  }

//
// @Test
// fun `camel snake case`() = assertSoftly {
//   val matcher = CamelSnakeCaseMatcher()
//
//   false -- null
//   false -- ""
//   false -- "    "
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "twowords"
//   false -- "twowords456"
//   false -- "twowordsabc"
//   false -- "twoabcwords"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "TWOWORDS"
//   false -- "TWOWORDS456"
//   false -- "TWOWORDSABC"
//   false -- "TWOABCWORDS"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "twoWords"
//   false -- "twoWords456"
//   false -- "twoWordsABC"
//   false -- "twoABCWords"
//
//   true -- "ONE_WORD_"
//   true -- "ONE_WORD_123"
//   false -- "TWO_WORDS"
//   false -- "TWO_WORDS456"
//   false -- "TWO_WORDSABC"
//   false -- "TwoABCWords"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "two_words"
//   false -- "two_words456"
//   false -- "two_words_abc"
//   false -- "two_abc_words"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "TWO_WORDS"
//   false -- "TWO_WORDS456"
//   false -- "TWO_WORDS_ABC"
//   false -- "TWO_ABC_WORDS"
//
//   true -- "ONE_WORD_"
//   true -- "ONE_WORD_123"
//   true -- "Two_Words"
//   true -- "Two_Words456"
//   true -- "Two_Words_ABC"
//   true -- "Two_ABC_Words"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "two-words"
//   false -- "two-words456"
//   false -- "two-words-abc"
//   false -- "two-abc-words"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "TWO-WORDS"
//   false -- "TWO-WORDS456"
//   false -- "TWO-WORDS-ABC"
//   false -- "TWO-ABC-WORDS"
//
//   true -- "ONE_WORD_"
//   true -- "ONE_WORD_123"
//   false -- "Two-Words"
//   false -- "Two-Words456"
//   false -- "Two-Words-ABC"
//   false -- "Two-ABC-Words"
// }
//
// @Test
// fun `kebab case`() = assertSoftly {
//   val matcher = KebabCaseMatcher()
//
//   false -- null
//   false -- ""
//   false -- "    "
//
//   true -- "ONE_WORD_"
//   true -- "ONE_WORD_123"
//   true -- "twowords"
//   true -- "twowords456"
//   true -- "twowordsabc"
//   true -- "twoabcwords"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "TWOWORDS"
//   false -- "TWOWORDS456"
//   false -- "TWOWORDSABC"
//   false -- "TWOABCWORDS"
//
//   true -- "ONE_WORD_"
//   true -- "ONE_WORD_123"
//   false -- "twoWords"
//   false -- "twoWords456"
//   false -- "twoWordsABC"
//   false -- "twoABCWords"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "TWO_WORDS"
//   false -- "TWO_WORDS456"
//   false -- "TWO_WORDSABC"
//   false -- "TwoABCWords"
//
//   true -- "ONE_WORD_"
//   true -- "ONE_WORD_123"
//   false -- "two_words"
//   false -- "two_words456"
//   false -- "two_words_abc"
//   false -- "two_abc_words"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "TWO_WORDS"
//   false -- "TWO_WORDS456"
//   false -- "TWO_WORDS_ABC"
//   false -- "TWO_ABC_WORDS"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "Two_Words"
//   false -- "Two_Words456"
//   false -- "Two_Words_ABC"
//   false -- "Two_ABC_Words"
//
//   true -- "ONE_WORD_"
//   true -- "ONE_WORD_123"
//   true -- "two-words"
//   true -- "two-words456"
//   true -- "two-words-abc"
//   true -- "two-abc-words"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "TWO-WORDS"
//   false -- "TWO-WORDS456"
//   false -- "TWO-WORDS-ABC"
//   false -- "TWO-ABC-WORDS"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "Two-Words"
//   false -- "Two-Words456"
//   false -- "Two-Words-ABC"
//   false -- "Two-ABC-Words"
// }
//
// @Test
// fun `screaming kebab case`() = assertSoftly {
//   val matcher = ScreamingKebabCaseMatcher()
//
//   false -- null
//   false -- ""
//   false -- "    "
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "twowords"
//   false -- "twowords456"
//   false -- "twowordsabc"
//   false -- "twoabcwords"
//
//   true -- "ONE_WORD_"
//   true -- "ONE_WORD_123"
//   true -- "TWOWORDS"
//   true -- "TWOWORDS456"
//   true -- "TWOWORDSABC"
//   true -- "TWOABCWORDS"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "twoWords"
//   false -- "twoWords456"
//   false -- "twoWordsABC"
//   false -- "twoABCWords"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "TWO_WORDS"
//   false -- "TWO_WORDS456"
//   false -- "TWO_WORDSABC"
//   false -- "TwoABCWords"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "two_words"
//   false -- "two_words456"
//   false -- "two_words_abc"
//   false -- "two_abc_words"
//
//   true -- "ONE_WORD_"
//   true -- "ONE_WORD_123"
//   false -- "TWO_WORDS"
//   false -- "TWO_WORDS456"
//   false -- "TWO_WORDS_ABC"
//   false -- "TWO_ABC_WORDS"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "Two_Words"
//   false -- "Two_Words456"
//   false -- "Two_Words_ABC"
//   false -- "Two_ABC_Words"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "two-words"
//   false -- "two-words456"
//   false -- "two-words-abc"
//   false -- "two-abc-words"
//
//   true -- "ONE_WORD_"
//   true -- "ONE_WORD_123"
//   true -- "TWO-WORDS"
//   true -- "TWO-WORDS456"
//   true -- "TWO-WORDS-ABC"
//   true -- "TWO-ABC-WORDS"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "Two-Words"
//   false -- "Two-Words456"
//   false -- "Two-Words-ABC"
//   false -- "Two-ABC-Words"
// }
//
// @Test
// fun `train case`() = assertSoftly {
//   val matcher = TrainCaseMatcher()
//
//   false -- null
//   false -- ""
//   false -- "    "
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "twowords"
//   false -- "twowords456"
//   false -- "twowordsabc"
//   false -- "twoabcwords"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "TWOWORDS"
//   false -- "TWOWORDS456"
//   false -- "TWOWORDSABC"
//   false -- "TWOABCWORDS"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "twoWords"
//   false -- "twoWords456"
//   false -- "twoWordsABC"
//   false -- "twoABCWords"
//
//   true -- "ONE_WORD_"
//   true -- "ONE_WORD_123"
//   false -- "TWO_WORDS"
//   false -- "TWO_WORDS456"
//   false -- "TWO_WORDSABC"
//   false -- "TwoABCWords"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "two_words"
//   false -- "two_words456"
//   false -- "two_words_abc"
//   false -- "two_abc_words"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "TWO_WORDS"
//   false -- "TWO_WORDS456"
//   false -- "TWO_WORDS_ABC"
//   false -- "TWO_ABC_WORDS"
//
//   true -- "ONE_WORD_"
//   true -- "ONE_WORD_123"
//   false -- "Two_Words"
//   false -- "Two_Words456"
//   false -- "Two_Words_ABC"
//   false -- "Two_ABC_Words"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "two-words"
//   false -- "two-words456"
//   false -- "two-words-abc"
//   false -- "two-abc-words"
//
//   false -- "ONE_WORD_"
//   false -- "ONE_WORD_123"
//   false -- "TWO-WORDS"
//   false -- "TWO-WORDS456"
//   false -- "TWO-WORDS-ABC"
//   false -- "TWO-ABC-WORDS"
//
//   true -- "ONE_WORD_"
//   true -- "ONE_WORD_123"
//   true -- "Two-Words"
//   true -- "Two-Words456"
//   true -- "Two-Words-ABC"
//   true -- "Two-ABC-Words"
// }

  fun List<String>.dynamic(
    extraName: String = "",
    test: (String) -> Unit
  ): List<DynamicTest> {
    return map { subject ->

      val displayName = if (extraName.isNotBlank()) {
        "'$subject' -- $extraName"
      } else subject

      DynamicTest.dynamicTest(displayName) {
        test.invoke(subject)
      }
    }
  }
}
