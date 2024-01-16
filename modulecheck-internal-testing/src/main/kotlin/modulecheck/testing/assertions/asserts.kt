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
package modulecheck.testing.assertions

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.fail

/**
 * Asserts that the receiver is not null. If it is null, throws
 * an [AssertionError] with the message provided by [lazyMessage].
 *
 * @param lazyMessage the function to provide a message in case the receiver is null.
 *   Default message states "The receiver cannot be null, but it was. ¯\\_(ツ)_/¯"
 * @return the receiver if it is not null
 * @throws AssertionError if the receiver is null
 */
fun <T : Any> T?.requireNotNullOrFail(
  lazyMessage: () -> String = { "The receiver cannot be null, but it was. ¯\\_(ツ)_/¯" }
): T {
  if (this != null) return this

  fail(lazyMessage)
}

/**
 * Asserts that the receiver string changes after calling
 * [String.replace] with [oldValue] and [replacement].
 *
 * @param oldValue the old value to be replaced
 * @param replacement the new value to replace the old value
 * @return the new string after replacement
 * @throws AssertionError if the string does not change after replacement
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
 * Asserts that the receiver string changes after calling
 * [String.replace] with [regex] and [replacement].
 *
 * @param regex the regular expression to be replaced
 * @param replacement the new value to replace the matched regular expression
 * @return the new string after replacement
 * @throws AssertionError if the string does not change after replacement
 */
fun String.replaceOrFail(regex: Regex, replacement: String): String {
  return assertChanged(
    oldString = this@replaceOrFail,
    newString = replace(regex, replacement),
    token = regex,
    replacement = replacement
  )
}

/**
 * Asserts that [oldString] changes after replacing a [token] with [replacement].
 *
 * @param oldString the original string before replacement
 * @param newString the new string after replacement
 * @param token the value that is replaced
 * @param replacement the new value that replaces the token
 * @return the new string
 * @throws AssertionError if the original string does not change after the replacement
 */
private fun assertChanged(oldString: String, newString: String, token: Any, replacement: String) =
  newString.also { new ->
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
    """.replaceIndentByMargin()
        .asClue { new shouldNotBe oldString }
    }
  }
