/*
 * Copyright (C) 2021-2025 Rick Busarow
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

package modulecheck.rule.impl

import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import modulecheck.config.ModuleCheckSettings
import modulecheck.dagger.DaggerList
import modulecheck.dagger.DaggerSet
import modulecheck.dagger.TaskScope
import modulecheck.rule.AllRules
import modulecheck.rule.ModuleCheckRule
import modulecheck.rule.RuleFilter

@Suppress("UndocumentedPublicClass", "UndocumentedPublicFunction")
@Module
@ContributesTo(TaskScope::class)
interface RuleModule {

  @Binds
  @IntoSet
  @AllRules
  fun bindAnvilFactoryRule(rule: AnvilFactoryRule): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun bindDepthRule(rule: DepthRule): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun bindDisableAndroidResourcesRule(rule: DisableAndroidResourcesRule): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun bindDisableViewBindingRule(rule: DisableViewBindingRule): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun bindInheritedDependencyRule(rule: InheritedDependencyRule): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun bindMustBeApiRule(rule: MustBeApiRule): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun bindOverShotDependencyRule(rule: OverShotDependencyRule): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun bindRedundantRule(rule: RedundantRule): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun bindSortDependenciesRule(rule: SortDependenciesRule): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun bindSortPluginsRule(rule: SortPluginsRule): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun bindUnusedDependencyRule(rule: UnusedDependencyRule): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun bindUnusedKaptPluginRule(rule: UnusedKaptPluginRule): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun bindUnusedKaptProcessorRule(rule: UnusedKaptProcessorRule): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun bindUnusedKotlinAndroidExtensionsRule(
    rule: UnusedKotlinAndroidExtensionsRule
  ): ModuleCheckRule<*>

  companion object {
    @Provides
    fun provideFilteredRules(
      @AllRules
      allRules: DaggerSet<ModuleCheckRule<*>>,
      ruleFilter: RuleFilter,
      settings: ModuleCheckSettings
    ): DaggerList<ModuleCheckRule<*>> = allRules.filter { ruleFilter.shouldEvaluate(it, settings) }
  }
}
