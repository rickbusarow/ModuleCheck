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
import modulecheck.api.settings.ChecksSettings
import modulecheck.api.settings.ModuleCheckExtension
import modulecheck.core.parser.lines
import org.junit.jupiter.api.Test
import kotlin.reflect.full.declaredMemberProperties

internal class RulesRegistrationTest : HermitJUnit5() {

  @Test
  fun `all rules should be represented in ChecksSettings`() {
    val settings = ModuleCheckExtension()

    val rules = ModuleCheckRuleFactory().create(settings)

    val ruleIds = rules
      .map { it.id.decapitalize() }
      .sorted()

    val checksProperties = ChecksSettings::class.declaredMemberProperties
      .map { it.name.decapitalize() }
      .sorted()

    val props = ChecksSettings::class.declaredMemberProperties
      .map { it.name to it.get(settings.checksSettings) }

    println(props.lines())

    checksProperties shouldBe ruleIds
  }
}
