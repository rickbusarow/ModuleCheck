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

import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.core.CouldUseAnvilFinding
import modulecheck.core.rule.ModuleCheckRule
import modulecheck.parsing.McProject

class AnvilFactoryRule(
  override val settings: ModuleCheckSettings
) : ModuleCheckRule<CouldUseAnvilFinding>() {

  override val id = "AnvilFactoryGeneration"
  override val description = "Finds modules which could use Anvil's factory generation " +
    "instead of Dagger's"

  override fun check(project: McProject): List<CouldUseAnvilFinding> {
    return AnvilFactoryParser.parse(project)
  }
}
