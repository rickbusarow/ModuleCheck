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

package modulecheck.api.test

import modulecheck.api.KaptMatcher
import modulecheck.api.settings.*

@Suppress("UNUSED_PARAMETER")
data class TestSettings(
  override var deleteUnused: Boolean = false,
  override var ignoreUnusedFinding: Set<String> = emptySet(),
  override var doNotCheck: Set<String> = emptySet(),
  override var additionalKaptMatchers: List<KaptMatcher> = emptyList(),
  override val checks: ChecksSettings = TestChecksSettings(),
  override val sort: SortSettings = TestSortSettings(),
  override val reports: ReportsSettings = TestReportsSettings()
) : ModuleCheckSettings {
  @Suppress("UNUSED")
  fun checks(block: ChecksSettings.() -> Unit) = Unit

  @Suppress("UNUSED")
  fun sort(block: SortSettings.() -> Unit) = Unit
}

@Suppress("LongParameterList")
data class TestChecksSettings(
  override var redundantDependency: Boolean = ChecksSettings.REDUNDANT_DEPENDENCY_DEFAULT,
  override var unusedDependency: Boolean = ChecksSettings.UNUSED_DEPENDENCY_DEFAULT,
  override var overShotDependency: Boolean = ChecksSettings.OVERSHOT_DEPENDENCY_DEFAULT,
  override var mustBeApi: Boolean = ChecksSettings.MUST_BE_API_DEFAULT,
  override var inheritedDependency: Boolean = ChecksSettings.INHERITED_DEPENDENCY_DEFAULT,
  override var sortDependencies: Boolean = ChecksSettings.SORT_DEPENDENCIES_DEFAULT,
  override var sortPlugins: Boolean = ChecksSettings.SORT_PLUGINS_DEFAULT,
  override var unusedKapt: Boolean = ChecksSettings.UNUSED_KAPT_DEFAULT,
  override var anvilFactoryGeneration: Boolean = ChecksSettings.ANVIL_FACTORY_GENERATION_DEFAULT,
  override var disableAndroidResources: Boolean = ChecksSettings.DISABLE_ANDROID_RESOURCES_DEFAULT,
  override var disableViewBinding: Boolean = ChecksSettings.DISABLE_VIEW_BINDING_DEFAULT,
  override var depths: Boolean = ChecksSettings.DEPTHS_DEFAULT
) : ChecksSettings

class TestSortSettings(
  override var pluginComparators: List<String> = SortSettings.PLUGIN_COMPARATORS_DEFAULT,
  override var dependencyComparators: List<String> = SortSettings.DEPENDENCY_COMPARATORS_DEFAULT
) : SortSettings

class TestReportsSettings(
  override val checkstyle: ReportSettings = TestReportSettings(
    ReportsSettings.CHECKSTYLE_ENABLED_DEFAULT,
    ReportsSettings.CHECKSTYLE_PATH_DEFAULT
  ),
  override val text: ReportSettings = TestReportSettings(
    ReportsSettings.TEXT_ENABLED_DEFAULT,
    ReportsSettings.TEXT_PATH_DEFAULT
  ),
  override val depths: ReportSettings = TestReportSettings(
    ReportsSettings.DEPTHS_ENABLED_DEFAULT,
    ReportsSettings.DEPTHS_PATH_DEFAULT
  )
) : ReportsSettings

class TestReportSettings(
  override var enabled: Boolean,
  override var outputPath: String
) : ReportSettings
