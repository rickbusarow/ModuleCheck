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
import modulecheck.api.settings.ChecksSettings
import modulecheck.api.settings.ChecksSettings.Companion.ANVIL_FACTORY_GENERATION_DEFAULT
import modulecheck.api.settings.ChecksSettings.Companion.DISABLE_ANDROID_RESOURCES_DEFAULT
import modulecheck.api.settings.ChecksSettings.Companion.DISABLE_VIEW_BINDING_DEFAULT
import modulecheck.api.settings.ChecksSettings.Companion.INHERITED_DEPENDENCY_DEFAULT
import modulecheck.api.settings.ChecksSettings.Companion.MUST_BE_API_DEFAULT
import modulecheck.api.settings.ChecksSettings.Companion.REDUNDANT_DEPENDENCY_DEFAULT
import modulecheck.api.settings.ChecksSettings.Companion.SORT_DEPENDENCIES_DEFAULT
import modulecheck.api.settings.ChecksSettings.Companion.SORT_PLUGINS_DEFAULT
import modulecheck.api.settings.ChecksSettings.Companion.UNUSED_DEPENDENCY_DEFAULT
import modulecheck.api.settings.ChecksSettings.Companion.UNUSED_KAPT_DEFAULT
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.api.settings.SortSettings
import modulecheck.api.settings.SortSettings.Companion.DEPENDENCY_COMPARATORS_DEFAULT
import modulecheck.api.settings.SortSettings.Companion.PLUGIN_COMPARATORS_DEFAULT
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

@Suppress("UNUSED_PARAMETER")
data class TestSettings(
  override var autoCorrect: Boolean = false,
  override var deleteUnused: Boolean = false,
  override var ignoreUnusedFinding: Set<String> = emptySet(),
  override var doNotCheck: Set<String> = emptySet(),
  override var additionalKaptMatchers: List<KaptMatcher> = emptyList(),
  override val checks: ChecksSettings = TestChecksSettings(),
  override val sort: SortSettings = TestSortSettings()
) : ModuleCheckSettings {
  @Suppress("UNUSED")
  fun checks(block: ChecksSettings.() -> Unit) = Unit

  @Suppress("UNUSED")
  fun sort(block: SortSettings.() -> Unit) = Unit
}

class TestChecksSettings(
  override var redundantDependency: Boolean = REDUNDANT_DEPENDENCY_DEFAULT,
  override var unusedDependency: Boolean = UNUSED_DEPENDENCY_DEFAULT,
  override var mustBeApi: Boolean = MUST_BE_API_DEFAULT,
  override var inheritedDependency: Boolean = INHERITED_DEPENDENCY_DEFAULT,
  override var sortDependencies: Boolean = SORT_DEPENDENCIES_DEFAULT,
  override var sortPlugins: Boolean = SORT_PLUGINS_DEFAULT,
  override var unusedKapt: Boolean = UNUSED_KAPT_DEFAULT,
  override var anvilFactoryGeneration: Boolean = ANVIL_FACTORY_GENERATION_DEFAULT,
  override var disableAndroidResources: Boolean = DISABLE_ANDROID_RESOURCES_DEFAULT,
  override var disableViewBinding: Boolean = DISABLE_VIEW_BINDING_DEFAULT
) : ChecksSettings

class TestSortSettings(
  override var pluginComparators: List<String> = PLUGIN_COMPARATORS_DEFAULT,
  override var dependencyComparators: List<String> = DEPENDENCY_COMPARATORS_DEFAULT
) : SortSettings
