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

package modulecheck.gradle

import modulecheck.api.KaptMatcher
import modulecheck.api.settings.ChecksSettings
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.api.settings.SortSettings
import modulecheck.core.rule.sort.SortDependenciesRule
import modulecheck.core.rule.sort.SortPluginsRule

open class ModuleCheckExtension : ModuleCheckSettings {

  override var autoCorrect: Boolean = true
  override var alwaysIgnore: Set<String> = mutableSetOf()
  override var ignoreAll: Set<String> = mutableSetOf()
  override var additionalKaptMatchers: List<KaptMatcher> = mutableListOf()

  override val checksSettings: ChecksExtension = ChecksExtension()
  override fun checks(block: ChecksSettings.() -> Unit) = block.invoke(checksSettings)

  override val sortSettings: SortExtension = SortExtension()
  override fun sort(block: SortSettings.() -> Unit) = block.invoke(sortSettings)
}

@Suppress("UnstableApiUsage")
class ChecksExtension : ChecksSettings {
  override var overshot: Boolean = true
  override var redundant: Boolean = false
  override var unused: Boolean = true
  override var mustBeApi: Boolean = true
  override var inheritedImplementation: Boolean = true
  override var used: Boolean = false
  override var sortDependencies: Boolean = false
  override var sortPlugins: Boolean = false
  override var kapt: Boolean = true
  override var anvilFactories: Boolean = true
  override var disableAndroidResources: Boolean = false
  override var disableViewBinding: Boolean = false
}

@Suppress("UnstableApiUsage")
class SortExtension : SortSettings {
  override var pluginComparators = SortPluginsRule.patterns
  override var dependencyComparators = SortDependenciesRule.patterns
}
