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

package modulecheck.builds.ktlint

import com.pinterest.ktlint.core.RuleProvider
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NoTrailingSpacesInRawStringLiteralRuleTest {

  val rules = setOf(
    RuleProvider { NoTrailingSpacesInRawStringLiteralRule() }
  )

  val TRIPLE = "\"\"\""

  @Test
  fun `trailing spaces in a string literal are removed`() {

    // Use the KotlinPoet dot ("·") so that:
    // (1) this rule doesn't clean up the whitespaces when running against the full project
    // (2) stuff's actually visible

    rules.format(
      """
      |const·val·name:·String·=·$TRIPLE
      |······
      |··fun·foo()·=·Unit··
      |··
      |$TRIPLE.trimIndent()
      |··
      """.trimMargin()
        .replace("·", " ")
    ) shouldBe
      """
      |const·val·name:·String·=·$TRIPLE
      |
      |  fun·foo()·=·Unit
      |
      |$TRIPLE.trimIndent()
      |··
      """.trimMargin()
        .replace("·", " ")
  }
}
