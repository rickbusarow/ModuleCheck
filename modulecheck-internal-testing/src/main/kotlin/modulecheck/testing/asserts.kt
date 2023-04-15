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

package modulecheck.testing

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.fail

fun <T : Any> T?.requireNotNullOrFail(
  lazyMessage: () -> String = { "The receiver cannot be null, but it was. ¯\\_(ツ)_/¯" }
): T {
  if (this != null) return this

  fail(lazyMessage)
}

/**
 * Asserts that the receiver string is changed after calling [String.replace]
 *
 * @since 0.12.4
 */
fun String.replaceOrFail(oldValue: String, replacement: String): String {
  return assertChanged(
    oldString = this@replaceOrFail,
    newString = replace(oldValue, replacement),
    token = oldValue,
    replacement = replacement
  )
}

/**
 * Asserts that the receiver string is changed after calling [String.replace]
 *
 * @since 0.12.4
 */
fun String.replaceOrFail(regex: Regex, replacement: String): String {
  return assertChanged(
    oldString = this@replaceOrFail,
    newString = replace(regex, replacement),
    token = regex,
    replacement = replacement
  )
}

private fun assertChanged(
  oldString: String,
  newString: String,
  token: Any,
  replacement: String
) = newString.also { new ->
  trimmedAssert {

    @Suppress("MagicNumber")
    val tokenName = (if (token is Regex) "regex" else "oldValue").padStart(9)

    """
      |String replacement did not change the original string.
      |
      |     $tokenName: $token
      |    replacement: $replacement
      |
      |original string (starting on the new line):
      |$oldString
      |____________________________________________________
      |""".replaceIndentByMargin()
      .asClue { new shouldNotBe oldString }
  }
}
