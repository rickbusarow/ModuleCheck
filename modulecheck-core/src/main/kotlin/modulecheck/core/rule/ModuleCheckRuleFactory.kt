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

import modulecheck.api.Finding
import modulecheck.api.ModuleCheckRule
import modulecheck.api.RuleFactory
import modulecheck.api.settings.ModuleCheckSettings

class ModuleCheckRuleFactory : RuleFactory {

  private val rules: MutableList<(ModuleCheckSettings) -> ModuleCheckRule<out Finding>> =
    mutableListOf(
      { DisableAndroidResourcesRule(it) },
      { DisableViewBindingRule(it) },
      { InheritedDependencyRule(it) },
      { MustBeApiRule(it) },
      { RedundantRule(it) },
      { SortDependenciesRule(it) },
      { SortPluginsRule(it) },
      { OverShotDependencyRule(it) },
      { UnusedDependencyRule(it) },
      { UnusedKaptRule(it) },
      { AnvilFactoryRule(it) }
    )

  override fun create(
    settings: ModuleCheckSettings
  ): List<ModuleCheckRule<out Finding>> {
    return rules.map { it.invoke(settings) }
  }
}
