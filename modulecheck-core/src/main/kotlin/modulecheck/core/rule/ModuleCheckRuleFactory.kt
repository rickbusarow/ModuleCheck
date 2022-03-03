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

package modulecheck.core.rule

import modulecheck.api.finding.Finding
import modulecheck.api.rule.ModuleCheckRule
import modulecheck.api.rule.RuleFactory
import modulecheck.api.settings.ModuleCheckSettings

class ModuleCheckRuleFactory : RuleFactory {

  private val rules: MutableList<(ModuleCheckSettings) -> ModuleCheckRule<out Finding>> =
    mutableListOf(
      { AnvilFactoryRule() },
      { DepthRule() },
      { DisableAndroidResourcesRule() },
      { DisableViewBindingRule() },
      { DisableKotlinAndroidExtensionsRule() },
      { InheritedDependencyRule() },
      { MustBeApiRule() },
      { OverShotDependencyRule(it) },
      { RedundantRule() },
      { SortDependenciesRule(it) },
      { SortPluginsRule(it) },
      { UnusedDependencyRule(it) },
      { UnusedKaptRule(it) }
    )

  override fun create(
    settings: ModuleCheckSettings
  ): List<ModuleCheckRule<out Finding>> {
    return rules.map { it.invoke(settings) }
  }
}
