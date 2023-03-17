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

import com.pinterest.ktlint.core.RuleProvider
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NoLeadingBlankLinesRuleTest {

  val rules = setOf(
    RuleProvider { NoLeadingBlankLinesRule() }
  )

  @Test
  fun `package declaration`() {

    rules.format(
      """
      |
      |
      |
      |package com.test
      |
      |class MyClass
      |
      """.trimMargin()
    ) shouldBe
      """
      |package com.test
      |
      |class MyClass
      |
      """.trimMargin()
  }

  @Test
  fun `file annotation`() {

    rules.format(
      """
      |
      |
      |
      |@file:Suppress("DEPRECATION")
      |
      |package com.test
      |
      |class MyClass
      |
      """.trimMargin()
    ) shouldBe
      """
      |@file:Suppress("DEPRECATION")
      |
      |package com.test
      |
      |class MyClass
      |
      """.trimMargin()
  }

  @Test
  fun `imports with no package declaration`() {

    rules.format(
      """
      |
      |
      |
      |import java.io.Serializable
      |
      |class MyClass : Serializable
      |
      """.trimMargin()
    ) shouldBe
      """
      |import java.io.Serializable
      |
      |class MyClass : Serializable
      |
      """.trimMargin()
  }

  @Test
  fun `code with no imports or package declaration`() {

    rules.format(
      """
      |
      |
      |
      |class MyClass
      |
      """.trimMargin()
    ) shouldBe
      """
      |class MyClass
      |
      """.trimMargin()
  }

  @Test
  fun `file license header`() {

    rules.format(
      """
      |
      |
      |
      |/*
      | * Copyright (C) 1985 Sylvester Stallone
      | */
      |
      |package com.test
      |
      |class MyClass
      |
      """.trimMargin()
    ) shouldBe
      """
      |/*
      | * Copyright (C) 1985 Sylvester Stallone
      | */
      |
      |package com.test
      |
      |class MyClass
      |
      """.trimMargin()
  }
}
