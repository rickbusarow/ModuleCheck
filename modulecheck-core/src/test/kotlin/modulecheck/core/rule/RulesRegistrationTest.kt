/*
 * Copyright (C) 2021 Rick Busarow
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

package modulecheck.core.rule

import hermit.test.junit.HermitJUnit5
import io.kotest.matchers.shouldBe
import modulecheck.api.KaptMatcher
import modulecheck.api.settings.*
import org.junit.jupiter.api.Test
import kotlin.reflect.full.declaredMemberProperties

internal class RulesRegistrationTest : HermitJUnit5() {

  @Test
  fun `all rules should be represented in ChecksSettings`() {
    val settings = TestSettings()

    val rules = ModuleCheckRuleFactory().create(settings)

    val ruleIds = rules
      .map {
        @Suppress("DEPRECATION") // we have to use `decapitalize()` for compatibility with Kotlin 1.4.x and Gradle < 7.0
        it.id.decapitalize()
      }
      .sorted()

    val checksProperties = ChecksSettings::class.declaredMemberProperties
      .map {
        @Suppress("DEPRECATION") // we have to use `decapitalize()` for compatibility with Kotlin 1.4.x and Gradle < 7.0
        it.name.decapitalize()
      }
      .filterNot { it == "anvilFactoryGeneration" } // Gradle plugin rule is only defined in the Gradle module
      .sorted()

    checksProperties shouldBe ruleIds
  }
}

data class TestSettings(
  override var autoCorrect: Boolean = false,
  override var ignoreUnusedFinding: Set<String> = emptySet(),
  override var doNotCheck: Set<String> = emptySet(),
  override var additionalKaptMatchers: List<KaptMatcher> = emptyList(),
  override val checks: ChecksSettings = ChecksExtension(),
  override val sort: SortSettings = SortExtension()
) : ModuleCheckSettings {
  override fun checks(block: ChecksSettings.() -> Unit) = Unit

  override fun sort(block: SortSettings.() -> Unit) = Unit
}
