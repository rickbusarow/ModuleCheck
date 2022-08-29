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
  fun AnvilFactoryRule.bindAnvilFactoryRule(): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun DepthRule.bindDepthRule(): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun DisableAndroidResourcesRule.bindDisableAndroidResourcesRule(): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun DisableViewBindingRule.bindDisableViewBindingRule(): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun InheritedDependencyRule.bindInheritedDependencyRule(): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun MustBeApiRule.bindMustBeApiRule(): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun OverShotDependencyRule.bindOverShotDependencyRule(): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun RedundantRule.bindRedundantRule(): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun SortDependenciesRule.bindSortDependenciesRule(): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun SortPluginsRule.bindSortPluginsRule(): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun UnusedDependencyRule.bindUnusedDependencyRule(): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun UnusedKaptPluginRule.bindUnusedKaptPluginRule(): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun UnusedKaptProcessorRule.bindUnusedKaptProcessorRule(): ModuleCheckRule<*>

  @Binds
  @IntoSet
  @AllRules
  fun UnusedKotlinAndroidExtensionsRule.bindUnusedKotlinAndroidExtensionsRule(): ModuleCheckRule<*>

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
