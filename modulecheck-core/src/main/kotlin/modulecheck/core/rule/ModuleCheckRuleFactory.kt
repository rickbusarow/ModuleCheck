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

import modulecheck.api.Fixable
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.core.overshot.OvershotRule
import modulecheck.core.rule.android.DisableAndroidResourcesRule
import modulecheck.core.rule.android.DisableViewBindingRule
import modulecheck.core.rule.sort.SortDependenciesRule
import modulecheck.core.rule.sort.SortPluginsRule

interface RuleFactory {

  fun create(settings: ModuleCheckSettings): List<ModuleCheckRule<*>>
  fun register(ruleFactory: (ModuleCheckSettings) -> ModuleCheckRule<out Fixable>)
}

class ModuleCheckRuleFactory : RuleFactory {

  private val rules: MutableList<(ModuleCheckSettings) -> ModuleCheckRule<out Fixable>> =
    mutableListOf(
      { DisableAndroidResourcesRule(it) },
      { DisableViewBindingRule(it) },
      { InheritedImplementationRule(it) },
      { MustBeApiRule(it) },
      { OvershotRule(it) },
      { RedundantRule(it) },
      { SortDependenciesRule(it) },
      { SortPluginsRule(it) },
      { UnusedDependencyRule(it) },
      { UnusedKaptRule(it) },
    )

  override fun create(
    settings: ModuleCheckSettings
  ): List<ModuleCheckRule<*>> {
    return rules.map { it.invoke(settings) }
  }

  override fun register(ruleFactory: (ModuleCheckSettings) -> ModuleCheckRule<out Fixable>) {
    rules.add(ruleFactory)
  }
}
