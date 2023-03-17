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

class NoSpaceInTargetedAnnotationRuleTest {

  val rules = setOf(
    RuleProvider { NoSpaceInTargetedAnnotationRule() }
  )

  @Test
  fun `space after colon`() {

    rules.format(
      """
      |@file: Suppress("DEPRECATION")
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
  fun `space before colon`() {

    rules.format(
      """
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
  fun `parameter list spaces are left alone`() {

    rules.format(
      """
      |@file:Suppress(
      |  "DEPRECATION"
      |)
      |
      |package com.test
      |
      |class MyClass
      |
      """.trimMargin()
    ) shouldBe
      """
      |@file:Suppress(
      |  "DEPRECATION"
      |)
      |
      |package com.test
      |
      |class MyClass
      |
      """.trimMargin()
  }
}
