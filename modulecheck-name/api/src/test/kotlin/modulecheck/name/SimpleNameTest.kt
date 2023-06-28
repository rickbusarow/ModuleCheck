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

package modulecheck.name

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.stringPattern
import modulecheck.testing.asTests
import modulecheck.testing.forAllBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

internal class SimpleNameTest {

  @TestFactory
  fun `valid java identifiers without backticks are allowed`() = listOf(
    "_test", "test", "t", "T", "\$var", "_", "$", "_123", "$123", "test123", "test_var", "TestVar",
    "TEST_VAR", "test\$var", "\$test_var", "_\$test", "T123", "_T123", "\$T123", "test$123",
    "Test$123", "TEST$123", "test_var123", "Test_var123", "TEST_VAR123", "_\$test123", "_Test123",
    "_TEST123", "\$test123", "\$Test123", "\$TEST123", "test\$var123", "Test\$var123",
    "TEST\$var123", "_1", "$1", "test1", "Test1", "TEST1", "test_var1", "Test_var1", "TEST_VAR1",
    "test\$var1", "Test\$var1", "TEST\$var1", "_test1", "_Test1", "_TEST1", "\$test1", "\$Test1",
    "validName", "_validName", "\$validName", "VALIDNAME", "Valid_Name", "_1234", "$1234",
    "name1234", "name_1234", "name$1234", "n", "_n", "\$n", "N", "n_1", "_a", "\$a", "a1", "a_1",
    "a$1", "b", "_b", "\$b", "B", "b_1", "_c", "\$c", "c1", "c_1", "c$1", "d", "_d", "\$d", "D",
    "d_1", "_e", "\$e", "e1", "e_1", "e$1", "f", "_f", "\$f", "F", "f_1", "_g", "\$g", "g1", "g_1",
    "g$1"
  )
    .asTests { name ->

      shouldNotThrow<Throwable> {
        SimpleName(name).asString shouldBe name
      }
    }

  @TestFactory
  fun `invalid java identifiers without backticks are not allowed`() = listOf(
    "1invalid", "#invalid", "@invalid", "!invalid", " invalid",
    "-invalid", "&invalid", "*invalid", "(invalid", ")invalid",
    "[invalid", "{invalid", "]invalid", "}invalid", ":invalid",
    ";invalid", ",invalid", "<invalid", ".invalid", ">invalid",
    "/invalid", "?invalid", "~invalid", "`invalid", "'invalid",
    "\"invalid", "|invalid", "\\invalid", "+invalid", "=invalid",
    "invalid ", "invalid\n", "invalid\t", "invalid\b", "invalid\\f",
    "invalid\r", "invalid\\v", "invalid\\x1B", "invalid\u0085", "invalid\u2028",
    "invalid\u2029", "invalid\uFEFF", "invalid\uFFF9", "invalid\uFFFA", "invalid\uFFFB",
    "invalid\uFFFC", "invalid\uFFFD", "invalid\uFFFE", "invalid\uFFFF", "invalid\u10000",
    "invalid\u10FFFF", "invalid\u1FFFFF", "invalid\u3FFFFFF", "invalid\u7FFFFFFF",
    "invalid\uFFFFFFFF"
  )
    .asTests { name ->

      shouldThrowWithMessage<IllegalArgumentException>(
        "SimpleName names must be valid Java identifier " +
          "without a dot qualifier.  This name was: `$name`"
      ) {
        SimpleName(name)
      }
    }

  @TestFactory
  fun `wrapped in backticks and allowed`() = listOf(
    "`validName`", "`_validName`", "`\$validName`", "`VALIDNAME`", "`Valid_Name`", "`_1234`",
    "`$1234`", "`name1234`", "`name_1234`", "`name$1234`", "`n`", "`_n`", "`\$n`", "`N`", "`n_1`",
    "`_a`", "`\$a`", "`a1`", "`a_1`", "`a$1`", "`b`", "`_b`", "`\$b`", "`B`", "`b_1`", "`_c`",
    "`\$c`", "`c1`", "`c_1`", "`c$1`", "`d`", "`_d`", "`\$d`", "`D`", "`d_1`", "`_e`", "`\$e`",
    "`e1`", "`e_1`", "`e$1`", "`f`", "`_f`", "`valid name`", "` validName`", "`validName `",
    "` valid Name `", "`valid  Name`", "`Name with spaces`", "`  multiple   spaces  `",
    "` leading space`", "`trailing space `", "` space around $ `", "` space around _ `",
    "` space before 1`", "`1 after space`", "` spaces around digits 123 `", "`single space`",
    "` multiple spaces `", "` space before and after `", "` space around keyword if `",
    "` space around keyword while `", "` space around keyword for `",
    "` space around keyword when `", "` space around keyword else `",
    "` space around keyword try `", "` space around keyword catch `",
    "` space around keyword finally `", "` space around keyword do `",
    "` space around keyword return `", "` space around keyword continue `",
    "` space around keyword break `", "` space around keyword class `",
    "` space around keyword object `", "` space around keyword interface `",
    "` space around keyword enum `", "` space around keyword annotation `",
    "` space around keyword typealias `", "` space around keyword constructor `",
    "` space around keyword by `", "` space around keyword get `", "` space around keyword set `",
    "` space around keyword import `", "` space around keyword as `", "` space around keyword is `",
    "` space around keyword in `", "` space around keyword out `",
    "` space around keyword override `", "` space around keyword public `",
    "` space around keyword private `", "` space around keyword internal `",
    "` space around keyword protected `"
  )
    .asTests { name ->

      shouldNotThrow<Throwable> {
        SimpleName(name).asString shouldBe name
      }
    }

  @TestFactory
  fun `wrapped in backticks but not allowed`() = listOf(
    "``",
    "`\n`",
    "`\nnewline`",
    "`newline\n`"
  )
    .asTests { name ->

      shouldThrowWithMessage<IllegalArgumentException>(
        "SimpleName names must be valid Java identifier " +
          "without a dot qualifier.  This name was: `$name`"
      ) {
        SimpleName(name)
      }
    }

  @TestFactory
  fun `any non-empty name in backticks without newlines or another backtick is allowed`() =
    Arb.stringPattern("""`[^\n\`]+`""")
      .asTests { name ->

        shouldNotThrow<Throwable> {
          SimpleName(name).asString shouldBe name
        }
      }

  @Test
  fun `an empty string in backticks is not allowed`() {

    val name = "``"

    shouldThrowWithMessage<IllegalArgumentException>(
      "SimpleName names must be valid Java identifier " +
        "without a dot qualifier.  This name was: `$name`"
    ) {
      SimpleName(name)
    }
  }

  @Test
  fun `a name with whitespaces is allowed if wrapped in backticks`() {

    val name = "`a name with whitespaces is allowed if wrapped in backticks`"

    shouldNotThrow<Throwable> {
      SimpleName(name).asString shouldBe name
    }
  }

  @TestFactory
  fun `a name without backticks with a white space is not allowed`() = Arb.stringPattern("""\s+""")
    .forAllBlocking { name ->

      shouldThrowWithMessage<IllegalArgumentException>(
        "SimpleName names must be valid Java identifier " +
          "without a dot qualifier.  This name was: `$name`"
      ) {
        SimpleName(name)
      }
    }

  @TestFactory
  fun `a name with a dot is not allowed`() = Arb.stringPattern("""\.+""")
    .forAllBlocking { name ->

      shouldThrowWithMessage<IllegalArgumentException>(
        "SimpleName names must be valid Java identifier " +
          "without a dot qualifier.  This name was: `$name`"
      ) {
        SimpleName(name)
      }
    }

  @Test
  fun `an empty name is not allowed`() {

    shouldThrowWithMessage<IllegalArgumentException>(
      "SimpleName names must be valid Java identifier " +
        "without a dot qualifier.  This name was: ``"
    ) {
      SimpleName("")
    }
  }

  @TestFactory
  fun `a blank name is not allowed`() = Arb.stringPattern("\\s*")
    .asTests(
      testName = { "`$it`" }
    ) { name ->

      shouldThrowWithMessage<IllegalArgumentException>(
        "SimpleName names must be valid Java identifier " +
          "without a dot qualifier.  This name was: `$name`"
      ) {
        SimpleName(name)
      }
    }

  @Test
  fun `an empty package name becomes DEFAULT`() {

    PackageName("") shouldBe PackageName.DEFAULT
  }

  @Test
  fun `a blank package name becomes DEFAULT`() {

    Arb.stringPattern("\\s*")
      .forAllBlocking { name ->

        PackageName(name) shouldBe PackageName.DEFAULT
      }
  }

  @Test
  fun `a backticked name with valid characters and spaces is allowed`() {
    val name = "`a valid name with spaces`"

    shouldNotThrow<Throwable> {
      SimpleName(name).asString shouldBe name
    }
  }

  @Test
  fun `a backticked name with a backtick inside it throws an exception`() {
    val name = "`a name ` with a backtick`"

    shouldThrowWithMessage<IllegalArgumentException>(
      "SimpleName names must be valid Java identifier " +
        "without a dot qualifier.  This name was: `$name`"
    ) {
      SimpleName(name)
    }
  }

  @Test
  fun `a backticked name with only spaces is allowed`() {
    val name = "`     `"

    shouldNotThrow<Throwable> {
      SimpleName(name).asString shouldBe name
    }
  }
}
