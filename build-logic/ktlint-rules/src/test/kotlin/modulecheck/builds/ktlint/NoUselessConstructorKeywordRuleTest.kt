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

class NoUselessConstructorKeywordRuleTest {

  val rules = setOf(
    RuleProvider { NoUselessConstructorKeywordRule() }
  )

  @Test
  fun `annotated constructor keyword is not removed`() {

    rules.format(
      """
      |class MyClass @Inject constructor(
      |  val name: String
      |)
      |
      """.trimMargin()
    ) shouldBe
      """
      |class MyClass @Inject constructor(
      |  val name: String
      |)
      |
      """.trimMargin()
  }

  @Test
  fun `private constructor keyword is not removed`() {

    rules.format(
      """
      |class MyClass private constructor(
      |  val name: String
      |)
      |
      """.trimMargin()
    ) shouldBe
      """
      |class MyClass private constructor(
      |  val name: String
      |)
      |
      """.trimMargin()
  }

  @Test
  fun `useless constructor keyword is removed`() {

    rules.format(
      """
      |class MyClass constructor(
      |  val name: String
      |)
      |
      """.trimMargin()
    ) shouldBe
      """
      |class MyClass(
      |  val name: String
      |)
      |
      """.trimMargin()
  }
}
